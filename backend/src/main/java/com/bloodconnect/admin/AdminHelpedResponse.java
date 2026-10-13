package com.bloodconnect.admin;

import com.bloodconnect.user.UserRole;

import java.time.Instant;
import java.util.UUID;

public record AdminHelpedResponse(UUID id, String patientName, String bloodGroup, int units, String hospital,
                                  String location, String urgency, String requestedBy, String requesterEmail,
                                  UserRole requesterRole, Instant createdAt) {
}
