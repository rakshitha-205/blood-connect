package com.bloodconnect.request;


import java.time.Instant;
import java.util.UUID;

public record RequestDeclineResponse(UUID id, String donorName, String donorBloodGroup, String reason,
                                     Instant createdAt) {

    public static RequestDeclineResponse from(RequestDecline decline) {
        return new RequestDeclineResponse(decline.getId(), decline.getDonor().getName(),
                decline.getDonor().getBloodGroup(), decline.getReason(), decline.getCreatedAt());
    }
}
