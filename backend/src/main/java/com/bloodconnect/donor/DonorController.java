package com.bloodconnect.donor;

import com.bloodconnect.alert.AcceptedRequestResponse;
import com.bloodconnect.alert.DeclineRequest;
import com.bloodconnect.alert.DonorAlertResponse;
import com.bloodconnect.alert.DonorAlertService;
import com.bloodconnect.matching.DonorMatchResponse;
import com.bloodconnect.user.AppUser;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Validated
@RestController
@RequestMapping("/api/donors")
@PreAuthorize("hasRole('DONOR')")
public class DonorController {

    private final DonorService donorService;
    private final DonorAlertService alertService;

    public DonorController(DonorService donorService, DonorAlertService alertService) {
        this.donorService = donorService;
        this.alertService = alertService;
    }

    @GetMapping("/me/alerts")
    public List<DonorAlertResponse> alerts(@AuthenticationPrincipal AppUser user) {
        return alertService.alertsFor(user.getId());
    }

    @PostMapping("/me/alerts/{requestId}/accept")
    public ResponseEntity<Void> accept(@AuthenticationPrincipal AppUser user, @PathVariable UUID requestId) {
        alertService.accept(user.getId(), requestId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me/accepted")
    public List<AcceptedRequestResponse> accepted(@AuthenticationPrincipal AppUser user) {
        return alertService.acceptedBy(user.getId());
    }

    @PostMapping("/me/alerts/{requestId}/decline")
    public ResponseEntity<Void> decline(@AuthenticationPrincipal AppUser user, @PathVariable UUID requestId,
                                        @Valid @RequestBody DeclineRequest request) {
        alertService.decline(user.getId(), requestId, request.reason());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/me")
    public DonorProfileResponse saveProfile(@AuthenticationPrincipal AppUser user,
                                            @Valid @RequestBody DonorProfileRequest request) {
        return donorService.save(user, request);
    }

    @GetMapping("/me")
    public DonorProfileResponse profile(@AuthenticationPrincipal AppUser user) {
        return donorService.get(user.getId());
    }

    @PatchMapping("/me/availability")
    public DonorProfileResponse availability(@AuthenticationPrincipal AppUser user,
                                             @Valid @RequestBody AvailabilityRequest request) {
        return donorService.setAvailability(user.getId(), request.available());
    }

    @GetMapping("/nearby")
    @PreAuthorize("hasAnyRole('REQUESTER', 'HOSPITAL')")
    public List<DonorMatchResponse> nearby(
            @RequestParam @Pattern(regexp = "A[+-]|B[+-]|AB[+-]|O[+-]") String bloodGroup,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude
    ) {
        return donorService.nearby(bloodGroup, latitude, longitude);
    }
}