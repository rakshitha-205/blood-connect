package com.bloodconnect.review;

import com.bloodconnect.user.UserRole;

import java.time.Instant;
import java.util.UUID;

public record ReviewResponse(UUID id, String reviewerName, UserRole reviewerRole, int rating, String comment,
                             Instant createdAt) {

    public static ReviewResponse from(Review review) {
        return new ReviewResponse(review.getId(), review.getReviewer().getName(), review.getReviewer().getRole(),
                review.getRating(), review.getComment(), review.getCreatedAt());
    }
}
