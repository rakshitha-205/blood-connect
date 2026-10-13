package com.bloodconnect.request;

import jakarta.validation.constraints.NotNull;

public record RequestStatusRequest(@NotNull RequestStatus status) {
}