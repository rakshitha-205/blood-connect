package com.bloodconnect.matching;

import com.bloodconnect.ai.OpenRouterClient;
import com.bloodconnect.request.BloodRequestResponse;
import com.bloodconnect.request.BloodRequestService;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class AiRankingService {

    static final int MAX_CANDIDATES = 15;
    private static final int MAX_REASON_LENGTH = 140;
    private static final Logger log = LoggerFactory.getLogger(AiRankingService.class);

    private static final String SYSTEM_PROMPT = """
            You rank blood donors for an emergency blood request.
            Order the donors from best to worst choice. Prefer, in this order:
            1. donors who are available,
            2. donors with the exact same blood group as the request,
            3. donors who are closer (unknown distance is worse than any known distance),
            4. donors with a higher match score.
            For critical requests, distance matters more than anything except availability.
            Reply with JSON only, no other text, in exactly this shape:
            {"ranking":[{"id":<donor id number>,"reason":"<why, at most 15 words>"}]}
            Include every donor id exactly once.""";

    private final BloodRequestService requestService;
    private final ObjectMapper objectMapper;
    private final OpenRouterClient openRouter;

    public AiRankingService(BloodRequestService requestService, ObjectMapper objectMapper, OpenRouterClient openRouter) {
        this.requestService = requestService;
        this.objectMapper = objectMapper;
        this.openRouter = openRouter;
    }

    public AiMatchRankingResponse rankForRequest(UUID requestId, UUID requesterId) {
        BloodRequestResponse request = requestService.get(requestId, requesterId);
        List<DonorMatchResponse> matches = requestService.matches(requestId, requesterId);
        return rank(request, matches);
    }

    AiMatchRankingResponse rank(BloodRequestResponse request, List<DonorMatchResponse> matches) {
        if (matches.size() < 2) {
            return byDistance(matches, null);
        }
        if (!openRouter.isEnabled()) {
            return byDistance(matches, "AI ranking is off because no OpenRouter API key is set. Showing nearest first.");
        }
        List<DonorMatchResponse> candidates = matches.subList(0, Math.min(MAX_CANDIDATES, matches.size()));
        try {
            String content = openRouter.ask(SYSTEM_PROMPT, buildPrompt(request, candidates), 0.2);
            List<RankedDonorMatch> ranked = new ArrayList<>(applyRanking(content, candidates));
            matches.subList(candidates.size(), matches.size())
                    .forEach(match -> ranked.add(RankedDonorMatch.from(match, null)));
            return new AiMatchRankingResponse(AiMatchRankingResponse.RANKED_BY_AI, openRouter.model(), null, ranked);
        } catch (Exception exception) {
            log.warn("AI donor ranking failed, falling back to distance order: {}", exception.getMessage());
            return byDistance(matches, "AI ranking is unavailable right now. Showing nearest first.");
        }
    }

    List<RankedDonorMatch> applyRanking(String content, List<DonorMatchResponse> candidates) throws Exception {
        int start = content.indexOf('{');
        int end = content.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new IllegalStateException("Model reply did not contain JSON");
        }
        JsonNode ranking = objectMapper.readTree(content.substring(start, end + 1)).path("ranking");
        if (!ranking.isArray() || ranking.isEmpty()) {
            throw new IllegalStateException("Model reply had no ranking");
        }

        List<RankedDonorMatch> ordered = new ArrayList<>();
        Set<Integer> used = new HashSet<>();
        for (JsonNode entry : ranking) {
            int id = entry.path("id").asInt(-1);
            if (id < 1 || id > candidates.size() || !used.add(id)) {
                continue;
            }
            ordered.add(RankedDonorMatch.from(candidates.get(id - 1), cleanReason(entry.path("reason").asText(""))));
        }
        if (ordered.isEmpty()) {
            throw new IllegalStateException("Model reply had no valid donor ids");
        }
        for (int index = 0; index < candidates.size(); index++) {
            if (!used.contains(index + 1)) {
                ordered.add(RankedDonorMatch.from(candidates.get(index), null));
            }
        }
        return ordered;
    }

    private String buildPrompt(BloodRequestResponse request, List<DonorMatchResponse> candidates) {
        StringBuilder prompt = new StringBuilder()
                .append("Request: blood group ").append(request.bloodGroup())
                .append(", units ").append(request.units())
                .append(", urgency ").append(request.urgency().toLowerCase(Locale.ROOT))
                .append(".\nDonors:\n");
        for (int index = 0; index < candidates.size(); index++) {
            DonorMatchResponse donor = candidates.get(index);
            prompt.append("- id ").append(index + 1)
                    .append(": blood group ").append(donor.bloodGroup())
                    .append(", distance ").append(donor.distanceKm() == null ? "unknown" : donor.distanceKm() + " km")
                    .append(", available ").append(donor.available() ? "yes" : "no")
                    .append(", match score ").append(donor.priorityScore()).append("/100\n");
        }
        return prompt.toString();
    }

    private static String cleanReason(String reason) {
        String trimmed = reason.replaceAll("\\s+", " ").trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.length() <= MAX_REASON_LENGTH ? trimmed : trimmed.substring(0, MAX_REASON_LENGTH - 1) + "…";
    }

    private static AiMatchRankingResponse byDistance(List<DonorMatchResponse> matches, String note) {
        return new AiMatchRankingResponse(AiMatchRankingResponse.RANKED_BY_DISTANCE, null, note,
                matches.stream().map(match -> RankedDonorMatch.from(match, null)).toList());
    }
}
