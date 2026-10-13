package com.bloodconnect.admin;

import java.time.Instant;
import java.util.UUID;

public record AdminHospitalResponse(UUID id, String name, String email, String phone,
                                    long requests, long helped, Instant createdAt) {
}
