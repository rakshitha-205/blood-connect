package com.bloodconnect.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

public record BloodRequestCreateRequest(
        @NotBlank @Size(max = 100) String patientName,
        @NotBlank @Pattern(regexp = "A[+-]|B[+-]|AB[+-]|O[+-]") String bloodGroup,
        @Min(1) @Max(20) int units,
        @NotBlank @Size(max = 120) String hospital,
        @NotBlank @Size(max = 160) String location,
        @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
        @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude,
        @NotBlank @Pattern(regexp = "Critical|Urgent|Normal", flags = Pattern.Flag.CASE_INSENSITIVE) String urgency,
        @NotBlank @Size(max = 30) String contact,
        @Size(max = 1000) String message
) {
}