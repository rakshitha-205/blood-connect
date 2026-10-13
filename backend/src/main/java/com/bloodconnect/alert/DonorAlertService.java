package com.bloodconnect.alert;

import com.bloodconnect.donor.DonorProfile;
import com.bloodconnect.donor.DonorProfileRepository;
import com.bloodconnect.matching.MatchingService;
import com.bloodconnect.request.BloodRequest;
import com.bloodconnect.request.BloodRequestRepository;
import com.bloodconnect.request.RequestAcceptance;
import com.bloodconnect.request.RequestAcceptanceRepository;
import com.bloodconnect.request.RequestDecline;
import com.bloodconnect.request.RequestDeclineRepository;
import com.bloodconnect.request.RequestStatus;
import com.bloodconnect.review.RatingSummary;
import com.bloodconnect.review.ReviewService;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class DonorAlertService {

    private static final Map<String, Integer> URGENCY_ORDER = Map.of("CRITICAL", 0, "URGENT", 1, "NORMAL", 2);

    private final DonorProfileRepository donorRepository;
    private final BloodRequestRepository requestRepository;
    private final RequestDeclineRepository declineRepository;
    private final RequestAcceptanceRepository acceptanceRepository;
    private final ReviewService reviewService;
    private final AlertNotifier alertNotifier;

    public DonorAlertService(DonorProfileRepository donorRepository, BloodRequestRepository requestRepository,
                             RequestDeclineRepository declineRepository, RequestAcceptanceRepository acceptanceRepository,
                             ReviewService reviewService, AlertNotifier alertNotifier) {
        this.donorRepository = donorRepository;
        this.requestRepository = requestRepository;
        this.declineRepository = declineRepository;
        this.acceptanceRepository = acceptanceRepository;
        this.reviewService = reviewService;
        this.alertNotifier = alertNotifier;
    }

    @Transactional(readOnly = true)
    public List<DonorAlertResponse> alertsFor(UUID donorUserId) {
        DonorProfile donor = findDonor(donorUserId);
        Set<UUID> answered = new HashSet<>();
        declineRepository.findAllByDonorId(donor.getId())
                .forEach(decline -> answered.add(decline.getRequest().getId()));
        acceptanceRepository.findAllByDonorIdOrderByCreatedAtDesc(donor.getId())
                .forEach(acceptance -> answered.add(acceptance.getRequest().getId()));
        List<BloodRequest> requests = requestRepository.findAllByStatus(RequestStatus.OPEN).stream()
                .filter(request -> !answered.contains(request.getId()))
                .filter(request -> MatchingService.isCompatible(request.getBloodGroup(), donor.getBloodGroup()))
                .toList();
        Map<UUID, RatingSummary> ratings = reviewService.ratingsFor(requests.stream()
                .map(request -> request.getRequester().getId())
                .collect(Collectors.toSet()));
        return requests.stream()
                .map(request -> DonorAlertResponse.from(request,
                        MatchingService.distanceKm(donor.getLatitude(), donor.getLongitude(),
                                request.getLatitude(), request.getLongitude()),
                        request.getBloodGroup().equals(donor.getBloodGroup()),
                        ratings.getOrDefault(request.getRequester().getId(), RatingSummary.NONE)))
                .sorted(Comparator
                        .comparingInt((DonorAlertResponse alert) -> URGENCY_ORDER.getOrDefault(alert.urgency(), 3))
                        .thenComparing(DonorAlertResponse::distanceKm, Comparator.nullsLast(Double::compareTo))
                        .thenComparing(DonorAlertResponse::createdAt, Comparator.reverseOrder()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AcceptedRequestResponse> acceptedBy(UUID donorUserId) {
        DonorProfile donor = findDonor(donorUserId);
        List<RequestAcceptance> acceptances = acceptanceRepository.findAllByDonorIdOrderByCreatedAtDesc(donor.getId());
        Map<UUID, RatingSummary> ratings = reviewService.ratingsFor(acceptances.stream()
                .map(acceptance -> acceptance.getRequest().getRequester().getId())
                .collect(Collectors.toSet()));
        return acceptances.stream()
                .map(acceptance -> {
                    BloodRequest request = acceptance.getRequest();
                    UUID requesterId = request.getRequester().getId();
                    return AcceptedRequestResponse.from(acceptance,
                            ratings.getOrDefault(requesterId, RatingSummary.NONE),
                            reviewService.revieweesOf(request.getId(), donorUserId).contains(requesterId));
                })
                .toList();
    }

    @Transactional
    public void accept(UUID donorUserId, UUID requestId) {
        DonorProfile donor = findDonor(donorUserId);
        BloodRequest request = findOpenRequest(requestId);
        if (!MatchingService.isCompatible(request.getBloodGroup(), donor.getBloodGroup())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Your blood group can't be given for this request");
        }
        if (donor.isDeferredOn(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Based on your eligibility check, you can't donate until " + donor.getDeferredUntil() + ".");
        }
        if (declineRepository.existsByRequestIdAndDonorId(requestId, donor.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You have already declined this request");
        }
        if (acceptanceRepository.existsByRequestIdAndDonorId(requestId, donor.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You have already accepted this request");
        }
        RequestAcceptance acceptance = acceptanceRepository.save(new RequestAcceptance(request, donor));
        alertNotifier.requestAccepted(acceptance);
    }

    @Transactional
    public void decline(UUID donorUserId, UUID requestId, String reason) {
        DonorProfile donor = findDonor(donorUserId);
        BloodRequest request = findOpenRequest(requestId);
        if (declineRepository.existsByRequestIdAndDonorId(requestId, donor.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You have already declined this request");
        }
        if (acceptanceRepository.existsByRequestIdAndDonorId(requestId, donor.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You have already accepted this request");
        }
        RequestDecline decline = new RequestDecline(request, donor, reason.trim());
        declineRepository.save(decline);
        alertNotifier.requestDeclined(decline);
    }

    private BloodRequest findOpenRequest(UUID requestId) {
        BloodRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Blood request not found"));
        if (request.getStatus() != RequestStatus.OPEN) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This request is no longer open");
        }
        return request;
    }

    private DonorProfile findDonor(UUID donorUserId) {
        return donorRepository.findByUserId(donorUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Donor profile not found"));
    }
}
