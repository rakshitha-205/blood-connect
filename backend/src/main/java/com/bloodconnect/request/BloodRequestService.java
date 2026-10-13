package com.bloodconnect.request;

import com.bloodconnect.alert.AlertNotifier;
import com.bloodconnect.matching.DonorMatchResponse;
import com.bloodconnect.matching.MatchingService;
import com.bloodconnect.user.AppUser;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class BloodRequestService {

    private final BloodRequestRepository requestRepository;
    private final MatchingService matchingService;

    private final RequestDeclineRepository declineRepository;
    private final AlertNotifier alertNotifier;

    public BloodRequestService(BloodRequestRepository requestRepository, MatchingService matchingService,
                               RequestDeclineRepository declineRepository, AlertNotifier alertNotifier) {
        this.requestRepository = requestRepository;
        this.matchingService = matchingService;
        this.declineRepository = declineRepository;
        this.alertNotifier = alertNotifier;
    }

    @Transactional(readOnly = true)
    public List<RequestDeclineResponse> declines(UUID requestId, UUID requesterId) {
        BloodRequest request = getOwnedRequest(requestId, requesterId);
        return declineRepository.findAllByRequestIdOrderByCreatedAtDesc(request.getId()).stream()
                .map(RequestDeclineResponse::from)
                .toList();
    }

    @Transactional
    public BloodRequestResponse create(AppUser user, BloodRequestCreateRequest request) {
        BloodRequest saved = requestRepository.save(new BloodRequest(user, request.patientName(), request.bloodGroup(),
                request.units(), request.hospital(), request.location(), request.latitude(), request.longitude(),
                request.urgency().toUpperCase(), request.contact(), request.message()));
        alertNotifier.requestCreated(saved, matchingService.availableDonorsFor(saved.getBloodGroup()).stream()
                .filter(donor -> !donor.getUser().getId().equals(user.getId()))
                .toList());
        return BloodRequestResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<DonorMatchResponse> matches(UUID requestId, UUID requesterId) {
        BloodRequest request = getOwnedRequest(requestId, requesterId);
        if (request.getStatus() != RequestStatus.OPEN) {
            return List.of();
        }
        Set<UUID> declinedDonors = declineRepository.findAllByRequestIdOrderByCreatedAtDesc(request.getId()).stream()
                .map(decline -> decline.getDonor().getId())
                .collect(Collectors.toSet());
        return matchingService.match(request).stream()
                .filter(match -> !declinedDonors.contains(match.id()))
                .toList();
    }

    @Transactional(readOnly = true)
    public BloodRequestResponse get(UUID requestId, UUID requesterId) {
        return BloodRequestResponse.from(getOwnedRequest(requestId, requesterId));
    }

    @Transactional
    public BloodRequestResponse updateStatus(UUID requestId, UUID requesterId, RequestStatus status) {
        BloodRequest request = getOwnedRequest(requestId, requesterId);
        request.setStatus(status);
        return BloodRequestResponse.from(request);
    }

    private BloodRequest getOwnedRequest(UUID requestId, UUID requesterId) {
        return requestRepository.findByIdAndRequesterId(requestId, requesterId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Blood request not found"));
    }
}