package com.bloodconnect.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record OtpVerifyRequest(
        @NotNull(message = "Choose email or phone.") OtpChannel channel,
        @NotBlank(message = "Enter the 6-digit code.")
        @Pattern(regexp = "\\d{6}", message = "Enter the 6-digit code.") String code
) {
}
