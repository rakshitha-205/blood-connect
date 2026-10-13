package com.bloodconnect.eligibility;

import java.time.LocalDate;
import java.util.List;

public record EligibilityResponse(
        boolean eligible,
        LocalDate deferredUntil,
        boolean seeDoctor,
        List<EligibilityReason> reasons,
        String explanation,
        String language,
        boolean aiUsed,
        String model,
        boolean noteSkipped,
        boolean savedToProfile
) {
}
