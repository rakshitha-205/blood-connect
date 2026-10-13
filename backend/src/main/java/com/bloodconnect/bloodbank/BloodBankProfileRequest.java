package com.bloodconnect.bloodbank;

import com.bloodconnect.auth.PhoneNumbers;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record BloodBankProfileRequest(
        @NotBlank(message = "Blood bank name is required.")
        @Size(max = 120, message = "Name must be at most 120 characters.") String name,
        @NotBlank(message = "Phone number is required.")
        @Pattern(regexp = PhoneNumbers.PATTERN, message = PhoneNumbers.MESSAGE) String phone,
        @NotBlank(message = "Location is required.")
        @Size(max = 160, message = "Location must be at most 160 characters.") String location,
        @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
        @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude,
        @Size(max = 80, message = "Opening hours must be at most 80 characters.") String openHours
) {
}
