package com.bloodconnect.admin;

import java.time.Instant;
import java.util.UUID;

/** bloodGroup, location and available are null when the donor has not finished their profile. */
public record AdminDonorResponse(UUID id, String name, String email, String phone,
                                 String bloodGroup, String location, Boolean available, Instant createdAt) {
}
