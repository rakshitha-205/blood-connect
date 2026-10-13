package com.bloodconnect.request;

import com.bloodconnect.matching.AiMatchRankingResponse;
import com.bloodconnect.matching.AiRankingService;
import com.bloodconnect.matching.DonorMatchResponse;
import com.bloodconnect.user.AppUser;

import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/requests")
@PreAuthorize("hasAnyRole('REQUESTER', 'HOSPITAL')")
public class BloodRequestController {

    private final BloodRequestService requestService;
    private final AiRankingService aiRankingService;
    private final RequestAcceptanceService acceptanceService;

    public BloodRequestController(BloodRequestService requestService, AiRankingService aiRankingService,
                                  RequestAcceptanceService acceptanceService) {
        this.requestService = requestService;
        this.aiRankingService = aiRankingService;
        this.acceptanceService = acceptanceService;
    }

    @PostMapping
    public org.springframework.http.ResponseEntity<BloodRequestResponse> create(
            @AuthenticationPrincipal AppUser user,
            @Valid @RequestBody BloodRequestCreateRequest request
    ) {
        return org.springframework.http.ResponseEntity.status(201).body(requestService.create(user, request));
    }

    @GetMapping("/{requestId}")
    public BloodRequestResponse get(@AuthenticationPrincipal AppUser user, @PathVariable UUID requestId) {
        return requestService.get(requestId, user.getId());
    }

    @GetMapping("/{requestId}/matches")
    public List<DonorMatchResponse> matches(@AuthenticationPrincipal AppUser user,
                                            @PathVariable UUID requestId) {
        return requestService.matches(requestId, user.getId());
    }

    @GetMapping("/{requestId}/declines")
    public List<RequestDeclineResponse> declines(@AuthenticationPrincipal AppUser user, @PathVariable UUID requestId) {
        return requestService.declines(requestId, user.getId());
    }

    @GetMapping("/{requestId}/acceptances")
    public List<RequestAcceptanceResponse> acceptances(@AuthenticationPrincipal AppUser user,
                                                       @PathVariable UUID requestId) {
        return acceptanceService.forRequester(requestId, user.getId());
    }

    @GetMapping("/{requestId}/matches/ai")
    public AiMatchRankingResponse aiMatches(@AuthenticationPrincipal AppUser user, @PathVariable UUID requestId) {
        return aiRankingService.rankForRequest(requestId, user.getId());
    }

    @PatchMapping("/{requestId}/status")
    public BloodRequestResponse updateStatus(@AuthenticationPrincipal AppUser user,
                                             @PathVariable UUID requestId,
                                             @Valid @RequestBody RequestStatusRequest request) {
        return requestService.updateStatus(requestId, user.getId(), request.status());
    }
}