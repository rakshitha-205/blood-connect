package com.bloodconnect.user;


import java.time.Instant;
import java.util.UUID;

public record UserResponse(UUID id, String fullName, String email, String phone, UserRole role, Instant createdAt,
                           boolean emailVerified, boolean phoneVerified) {
    public static UserResponse from(AppUser user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getPhone(), user.getRole(),
                user.getCreatedAt(), user.isEmailVerified(), user.isPhoneVerified());
    }
}
