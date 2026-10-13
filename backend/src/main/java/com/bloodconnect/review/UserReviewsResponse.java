package com.bloodconnect.review;

import java.util.List;

public record UserReviewsResponse(Double average, long count, List<ReviewResponse> reviews) {
}
