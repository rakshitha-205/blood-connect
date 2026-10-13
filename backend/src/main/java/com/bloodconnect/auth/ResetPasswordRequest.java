package com.bloodconnect.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank(message = "Email is required.")
        @Email(message = "Enter a valid email address.")
        @Size(max = 254, message = "Email must be at most 254 characters.") String email,
        @NotBlank(message = "Enter the 6-digit code.")
        @Pattern(regexp = "\\d{6}", message = "Enter the 6-digit code.") String code,
        @NotBlank(message = "Enter a new password.")
        @Size(min = 10, max = 72, message = "Password must be 10 to 72 characters long.") String newPassword
) {
}
