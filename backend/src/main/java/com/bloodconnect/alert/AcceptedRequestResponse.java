package com.bloodconnect.alert;

import com.bloodconnect.request.BloodRequest;
import com.bloodconnect.request.RequestAcceptance;
import com.bloodconnect.request.RequestStatus;
import com.bloodconnect.review.RatingSummary;

import java.time.Instant;
import java.util.UUID;

public record AcceptedRequestResponse(UUID requestId, String patientName, String bloodGroup, int units,
                                      String hospital, String location, Double latitude, Double longitude,
                                      String urgency, String contact, RequestStatus status, Instant acceptedAt,
                                      UUID requesterId, String requesterName, Double requesterRating,
                                      long requesterRatingCount, boolean reviewed) {
    public static AcceptedRequestResponse from(RequestAcceptance acceptance, RatingSummary requesterRating,
                                               boolean reviewed) {
        BloodRequest request = acceptance.getRequest();
        return new AcceptedRequestResponse(request.getId(), request.getPatientName(), request.getBloodGroup(),
                request.getUnits(), request.getHospital(), request.getLocation(), request.getLatitude(),
                request.getLongitude(), request.getUrgency(), request.getContact(), request.getStatus(),
                acceptance.getCreatedAt(), request.getRequester().getId(), request.getRequester().getName(),
                requesterRating.average(), requesterRating.count(), reviewed);
    }
}
