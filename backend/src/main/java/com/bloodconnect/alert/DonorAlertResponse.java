package com.bloodconnect.alert;

import com.bloodconnect.request.BloodRequest;
import com.bloodconnect.review.RatingSummary;

import java.time.Instant;
import java.util.UUID;

public record DonorAlertResponse(UUID requestId, String patientName, String bloodGroup, int units, String hospital,
                                 String location, Double latitude, Double longitude, String urgency, String contact,
                                 String message, Instant createdAt, Double distanceKm, boolean exactMatch,
                                 Double requesterRating, long requesterRatingCount) {
    public static DonorAlertResponse from(BloodRequest request, Double distanceKm, boolean exactMatch,
                                          RatingSummary requesterRating) {
        return new DonorAlertResponse(request.getId(), request.getPatientName(), request.getBloodGroup(),
                request.getUnits(), request.getHospital(), request.getLocation(), request.getLatitude(),
                request.getLongitude(), request.getUrgency(), request.getContact(), request.getMessage(),
                request.getCreatedAt(), distanceKm, exactMatch, requesterRating.average(), requesterRating.count());
    }
}
