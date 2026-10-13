package com.bloodconnect.matching;

import com.bloodconnect.donor.DonorProfile;
import com.bloodconnect.review.RatingSummary;

import java.util.UUID;

public record DonorMatchResponse(UUID id, String name, String bloodGroup, String location,
                                Double latitude, Double longitude, Double distanceKm,
                                int priorityScore, boolean available, Double rating, long ratingCount) {
    public static DonorMatchResponse from(DonorProfile donor, Double distanceKm, int priorityScore, RatingSummary rating) {
        return new DonorMatchResponse(donor.getId(), donor.getName(), donor.getBloodGroup(), donor.getLocation(),
                donor.getLatitude(), donor.getLongitude(), distanceKm, priorityScore, donor.isAvailable(),
                rating.average(), rating.count());
    }
}
