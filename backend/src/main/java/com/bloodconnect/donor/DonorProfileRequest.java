package com.bloodconnect.donor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

public record DonorProfileRequest(
        @NotBlank(message = "Name is required.")
        @Size(max = 100, message = "Name must be at most 100 characters.") String name,
        @NotBlank(message = "Phone number is required.")
        @Size(max = 30, message = "Phone number must be at most 30 characters.") String phone,
        @NotBlank(message = "Blood group is required.")
        @Pattern(regexp = "A[+-]|B[+-]|AB[+-]|O[+-]", message = "Choose a valid blood group.") String bloodGroup,
        @NotBlank(message = "Location is required.")
        @Size(max = 160, message = "Location must be at most 160 characters.") String location,
        @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
        @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude,
        @NotNull(message = "Choose your availability.") Boolean available
) {
}
