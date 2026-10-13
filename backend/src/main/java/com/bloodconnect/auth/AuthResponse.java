package com.bloodconnect.auth;

import com.bloodconnect.user.UserResponse;

public record AuthResponse(String tokenType, String accessToken, long expiresInSeconds, UserResponse user) {
}
