package com.bloodconnect.review;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record ReviewCreateRequest(
        @NotNull UUID requestId,
        UUID revieweeId,
        @Min(1) @Max(5) int rating,
        @Size(max = 500) String comment
) {
}
