package com.bloodconnect.auth;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record OtpSendRequest(
        @NotNull(message = "Choose email or phone.") OtpChannel channel,
        @Pattern(regexp = PhoneNumbers.PATTERN, message = PhoneNumbers.MESSAGE) String phone
) {
}
