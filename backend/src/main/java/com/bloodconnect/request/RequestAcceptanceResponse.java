package com.bloodconnect.request;

import com.bloodconnect.donor.DonorProfile;
import com.bloodconnect.matching.MatchingService;
import com.bloodconnect.review.RatingSummary;

import java.time.Instant;
import java.util.UUID;

public record RequestAcceptanceResponse(UUID donorUserId, String name, String bloodGroup, String phone,
                                        String location, Double distanceKm, Instant acceptedAt,
                                        Double rating, long ratingCount, boolean reviewed) {
    public static RequestAcceptanceResponse from(RequestAcceptance acceptance, RatingSummary rating, boolean reviewed) {
        DonorProfile donor = acceptance.getDonor();
        BloodRequest request = acceptance.getRequest();
        return new RequestAcceptanceResponse(donor.getUser().getId(), donor.getName(), donor.getBloodGroup(),
                donor.getPhone(), donor.getLocation(),
                MatchingService.distanceKm(request.getLatitude(), request.getLongitude(),
                        donor.getLatitude(), donor.getLongitude()),
                acceptance.getCreatedAt(), rating.average(), rating.count(), reviewed);
    }
}
