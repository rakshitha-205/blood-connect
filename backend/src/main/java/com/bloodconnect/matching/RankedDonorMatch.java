package com.bloodconnect.matching;

import java.util.UUID;

public record RankedDonorMatch(UUID id, String name, String bloodGroup, String location,
                               Double latitude, Double longitude, Double distanceKm,
                               int priorityScore, boolean available, Double rating, long ratingCount,
                               String aiReason) {
    public static RankedDonorMatch from(DonorMatchResponse match, String aiReason) {
        return new RankedDonorMatch(match.id(), match.name(), match.bloodGroup(), match.location(),
                match.latitude(), match.longitude(), match.distanceKm(), match.priorityScore(),
                match.available(), match.rating(), match.ratingCount(), aiReason);
    }
}
