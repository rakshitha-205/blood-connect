package com.bloodconnect.eligibility;

import java.time.Period;

/**
 * Reasons a donor may have to wait, with the waiting period from the NBTC (India) donor selection guidelines.
 * A null wait means there is no fixed date: the donor should not donate now and should ask a doctor at the blood bank.
 */
public enum EligibilityCondition {

    AGE_OUT_OF_RANGE(null, false, "Blood donors must be between 18 and 65 years old."),
    LOW_WEIGHT(null, false, "Blood donors must weigh at least 45 kg."),
    RECENT_DONATION(null, false,
            "There must be at least 90 days (men) or 120 days (women) between whole blood donations."),
    TATTOO_OR_PIERCING(Period.ofMonths(12), true,
            "Tattoos, body piercing and acupuncture need a 12-month wait before donating."),
    MINOR_SURGERY(Period.ofMonths(6), true, "Minor surgery needs a 6-month wait before donating."),
    MAJOR_SURGERY(Period.ofMonths(12), true, "Major surgery needs a 12-month wait before donating."),
    FEVER_OR_ILLNESS(Period.ofDays(14), true,
            "Wait 2 weeks after you have fully recovered from a fever or infection."),
    COLD_OR_FLU(Period.ofDays(7), true, "Wait 7 days after a cold, cough or flu has fully cleared."),
    ANTIBIOTICS(Period.ofDays(7), true, "Wait 7 days after your last dose of antibiotics."),
    ALCOHOL(Period.ofDays(1), true, "Wait 24 hours after drinking alcohol."),
    PREGNANT_OR_BREASTFEEDING(null, true,
            "You cannot donate while pregnant or breastfeeding, or within 12 months of delivery."),
    MALARIA(Period.ofMonths(3), true, "Wait 3 months after full recovery from malaria."),
    DENTAL_WORK(Period.ofDays(3), true, "Wait 3 days after dental treatment or a tooth extraction."),
    RECENT_VACCINATION(Period.ofDays(28), true, "Wait 4 weeks after a live vaccine (such as MMR or yellow fever)."),
    JAUNDICE(Period.ofMonths(12), true, "Wait 12 months after recovering from jaundice."),
    SERIOUS_CONDITION(null, true,
            "Some long-term conditions (such as heart disease, cancer, hepatitis B or C, HIV or epilepsy) "
                    + "need a doctor's advice at the blood bank before donating.");

    private final Period wait;
    private final boolean fromNote;
    private final String description;

    EligibilityCondition(Period wait, boolean fromNote, String description) {
        this.wait = wait;
        this.fromNote = fromNote;
        this.description = description;
    }

    public Period waitPeriod() { return wait; }
    public boolean allowedFromNote() { return fromNote; }
    public String description() { return description; }
}
