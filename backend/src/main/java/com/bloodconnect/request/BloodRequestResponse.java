package com.bloodconnect.request;


import java.time.Instant;
import java.util.UUID;

public record BloodRequestResponse(UUID id, String patientName, String bloodGroup, int units, String hospital,
                                  String location, Double latitude, Double longitude, String urgency,
                                  String contact, String message, RequestStatus status, Instant createdAt) {
    public static BloodRequestResponse from(BloodRequest request) {
        return new BloodRequestResponse(request.getId(), request.getPatientName(), request.getBloodGroup(),
                request.getUnits(), request.getHospital(), request.getLocation(), request.getLatitude(),
                request.getLongitude(), request.getUrgency(), request.getContact(), request.getMessage(),
                request.getStatus(), request.getCreatedAt());
    }
}