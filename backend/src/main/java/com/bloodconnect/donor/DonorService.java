package com.bloodconnect.donor;

import com.bloodconnect.matching.DonorMatchResponse;
import com.bloodconnect.matching.MatchingService;
import com.bloodconnect.user.AppUser;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class DonorService {

    private static final DateTimeFormatter DEFERRED_DATE = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH);

    private final DonorProfileRepository donorRepository;
    private final MatchingService matchingService;

    public DonorService(DonorProfileRepository donorRepository, MatchingService matchingService) {
        this.donorRepository = donorRepository;
        this.matchingService = matchingService;
    }

    @Transactional
    public DonorProfileResponse save(AppUser user, DonorProfileRequest request) {
        DonorProfile profile = donorRepository.findByUserId(user.getId())
                .orElseGet(() -> new DonorProfile(user, request.name(), request.phone(), request.bloodGroup(),
                        request.location(), request.latitude(), request.longitude(), request.available()));
        profile.update(request.name(), request.phone(), request.bloodGroup(), request.location(),
                request.latitude(), request.longitude(), request.available());
        return DonorProfileResponse.from(donorRepository.save(profile));
    }

    @Transactional(readOnly = true)
    public DonorProfileResponse get(UUID userId) {
        return donorRepository.findByUserId(userId).map(DonorProfileResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Donor profile not found"));
    }

    @Transactional
    public DonorProfileResponse setAvailability(UUID userId, boolean available) {
        DonorProfile profile = donorRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Donor profile not found"));
        if (available && profile.isDeferredOn(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Based on your eligibility check, you can donate again from "
                    + DEFERRED_DATE.format(profile.getDeferredUntil()) + ".");
        }
        profile.setAvailable(available);
        return DonorProfileResponse.from(profile);
    }

    @Transactional
    public boolean recordEligibility(UUID userId, LocalDate deferredUntil, boolean seeDoctor) {
        return donorRepository.findByUserId(userId)
                .map(profile -> {
                    profile.recordEligibility(deferredUntil, seeDoctor);
                    return true;
                })
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public List<DonorMatchResponse> nearby(String bloodGroup, Double latitude, Double longitude) {
        return matchingService.findMatches(bloodGroup, latitude, longitude);
    }
}