package com.bloodconnect.eligibility;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** Health answers for one check. A date means "yes, and this is when it happened or ended"; null means "no". */
public record EligibilityRequest(
        @Pattern(regexp = "en|hi|te|kn|ta", message = "Choose English, Hindi, Telugu, Kannada or Tamil") String language,
        @NotNull(message = "Enter your age")
        @Min(value = 1, message = "Enter a valid age") @Max(value = 120, message = "Enter a valid age") Integer age,
        @NotNull(message = "Enter your weight")
        @DecimalMin(value = "10", message = "Enter a valid weight")
        @DecimalMax(value = "300", message = "Enter a valid weight") Double weightKg,
        @NotNull(message = "Choose male or female")
        @Pattern(regexp = "MALE|FEMALE", message = "Choose male or female") String sex,
        LocalDate lastDonationDate,
        LocalDate tattooOrPiercingDate,
        LocalDate minorSurgeryDate,
        LocalDate majorSurgeryDate,
        LocalDate feverRecoveryDate,
        LocalDate antibioticsLastDoseDate,
        LocalDate malariaRecoveryDate,
        LocalDate dentalWorkDate,
        boolean alcoholLast24Hours,
        boolean pregnantOrBreastfeeding,
        @Size(max = 500, message = "Keep the note under 500 characters") String note
) {
    public String languageOrDefault() {
        return language == null || language.isBlank() ? "en" : language;
    }
}
