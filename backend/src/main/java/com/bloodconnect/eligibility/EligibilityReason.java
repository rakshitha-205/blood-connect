package com.bloodconnect.eligibility;

import java.time.LocalDate;

/** One reason to wait. {@code until} is the first day the donor can donate again, or null when a doctor must decide. */
public record EligibilityReason(EligibilityCondition code, LocalDate until, String source, String description) {

    public static final String FROM_FORM = "FORM";
    public static final String FROM_NOTE = "NOTE";

    static EligibilityReason of(EligibilityCondition code, LocalDate until, String source) {
        return new EligibilityReason(code, until, source, code.description());
    }
}
