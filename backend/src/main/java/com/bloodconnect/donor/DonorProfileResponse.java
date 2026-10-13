package com.bloodconnect.donor;


import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record DonorProfileResponse(UUID id, String name, String phone, String bloodGroup, String location,
                                   Double latitude, Double longitude, boolean available, LocalDate deferredUntil,
                                   Instant eligibilityCheckedAt, Instant updatedAt) {
    public static DonorProfileResponse from(DonorProfile profile) {
        LocalDate deferredUntil = profile.isDeferredOn(LocalDate.now()) ? profile.getDeferredUntil() : null;
        return new DonorProfileResponse(profile.getId(), profile.getName(), profile.getPhone(),
                profile.getBloodGroup(), profile.getLocation(), profile.getLatitude(), profile.getLongitude(),
                profile.isAvailable(), deferredUntil, profile.getEligibilityCheckedAt(), profile.getUpdatedAt());
    }
}
