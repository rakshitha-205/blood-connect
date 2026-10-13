package com.bloodconnect.request;

import com.bloodconnect.review.RatingSummary;
import com.bloodconnect.review.ReviewService;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class RequestAcceptanceService {

    private final BloodRequestRepository requestRepository;
    private final RequestAcceptanceRepository acceptanceRepository;
    private final ReviewService reviewService;

    public RequestAcceptanceService(BloodRequestRepository requestRepository,
                                    RequestAcceptanceRepository acceptanceRepository, ReviewService reviewService) {
        this.requestRepository = requestRepository;
        this.acceptanceRepository = acceptanceRepository;
        this.reviewService = reviewService;
    }

    @Transactional(readOnly = true)
    public List<RequestAcceptanceResponse> forRequester(UUID requestId, UUID requesterId) {
        BloodRequest request = requestRepository.findByIdAndRequesterId(requestId, requesterId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Blood request not found"));
        List<RequestAcceptance> acceptances = acceptanceRepository.findAllByRequestIdOrderByCreatedAtAsc(request.getId());
        Map<UUID, RatingSummary> ratings = reviewService.ratingsFor(acceptances.stream()
                .map(acceptance -> acceptance.getDonor().getUser().getId())
                .collect(Collectors.toSet()));
        Set<UUID> reviewed = reviewService.revieweesOf(request.getId(), requesterId);
        return acceptances.stream()
                .map(acceptance -> {
                    UUID donorUserId = acceptance.getDonor().getUser().getId();
                    return RequestAcceptanceResponse.from(acceptance,
                            ratings.getOrDefault(donorUserId, RatingSummary.NONE), reviewed.contains(donorUserId));
                })
                .toList();
    }
}
