package com.bloodconnect.auth;

import com.bloodconnect.user.UserRole;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Full name is required.")
        @Size(max = 100, message = "Name must be at most 100 characters.") String name,
        @NotBlank(message = "Email is required.")
        @Email(message = "Enter a valid email address.")
        @Size(max = 254, message = "Email must be at most 254 characters.") String email,
        @NotBlank(message = "Phone number is required.")
        @Pattern(regexp = PhoneNumbers.PATTERN, message = PhoneNumbers.MESSAGE) String phone,
        @NotBlank(message = "Password is required.")
        @Size(min = 10, max = 72, message = "Password must be 10 to 72 characters long.") String password,
        @NotNull(message = "Choose an account type.") UserRole role
) {
}
