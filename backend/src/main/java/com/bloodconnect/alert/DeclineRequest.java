package com.bloodconnect.alert;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DeclineRequest(
        @NotBlank(message = "Tell the patient why you can't donate")
        @Size(max = 300, message = "Keep the reason under 300 characters")
        String reason
) {
}
