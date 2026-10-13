package com.bloodconnect.matching;

import com.bloodconnect.ai.OpenRouterClient;
import com.bloodconnect.request.BloodRequestResponse;
import com.bloodconnect.request.BloodRequestService;
import com.bloodconnect.request.RequestStatus;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class AiRankingServiceTest {

    private final DonorMatchResponse near = donor("Near", 2.0, true);
    private final DonorMatchResponse mid = donor("Mid", 10.0, false);
    private final DonorMatchResponse far = donor("Far", 90.0, true);
    private final List<DonorMatchResponse> matches = List.of(near, mid, far);

    @Test
    void withoutApiKeyKeepsDistanceOrderAndExplainsWhy() {
        AiMatchRankingResponse response = service("", "http://127.0.0.1:9").rank(request(), matches);

        assertThat(response.rankedBy()).isEqualTo(AiMatchRankingResponse.RANKED_BY_DISTANCE);
        assertThat(response.note()).contains("no OpenRouter API key");
        assertThat(response.matches()).extracting(RankedDonorMatch::name).containsExactly("Near", "Mid", "Far");
    }

    @Test
    void unreachableModelFallsBackToDistanceOrder() {
        AiMatchRankingResponse response = service("test-key", "http://127.0.0.1:9").rank(request(), matches);

        assertThat(response.rankedBy()).isEqualTo(AiMatchRankingResponse.RANKED_BY_DISTANCE);
        assertThat(response.note()).contains("unavailable");
        assertThat(response.matches()).extracting(RankedDonorMatch::name).containsExactly("Near", "Mid", "Far");
    }

    @Test
    void appliesModelOrderAndFillsMissingDonorsNearestFirst() throws Exception {
        String reply = """
                Here you go:
                {"ranking":[{"id":3,"reason":"Available exact match"},{"id":3,"reason":"duplicate"},{"id":7,"reason":"bad"}]}
                """;

        List<RankedDonorMatch> ranked = service("", "http://127.0.0.1:9").applyRanking(reply, matches);

        assertThat(ranked).extracting(RankedDonorMatch::name).containsExactly("Far", "Near", "Mid");
        assertThat(ranked.get(0).aiReason()).isEqualTo("Available exact match");
        assertThat(ranked.get(1).aiReason()).isNull();
    }

    @Test
    void rejectsReplyWithoutJson() {
        assertThatThrownBy(() -> service("", "http://127.0.0.1:9").applyRanking("no idea", matches))
                .isInstanceOf(IllegalStateException.class);
    }

    private AiRankingService service(String apiKey, String baseUrl) {
        return new AiRankingService(mock(BloodRequestService.class), new ObjectMapper(),
                new OpenRouterClient(apiKey, "test/model:free", baseUrl, Duration.ofSeconds(2)));
    }

    private static BloodRequestResponse request() {
        return new BloodRequestResponse(UUID.randomUUID(), "Patient", "O-", 2, "City Hospital", "Madhapur",
                17.44, 78.39, "CRITICAL", "9999999999", null, RequestStatus.OPEN, Instant.now());
    }

    private static DonorMatchResponse donor(String name, Double distanceKm, boolean available) {
        return new DonorMatchResponse(UUID.randomUUID(), name, "O-", name, 17.4, 78.4, distanceKm, 80, available, null, 0);
    }
}
