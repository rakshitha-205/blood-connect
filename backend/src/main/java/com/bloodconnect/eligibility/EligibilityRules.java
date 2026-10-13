package com.bloodconnect.eligibility;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Decides eligibility from fixed NBTC rules. The AI never changes this decision. */
public final class EligibilityRules {

    static final int MIN_AGE = 18;
    static final int MAX_AGE = 65;
    static final double MIN_WEIGHT_KG = 45;
    static final int MALE_DONATION_GAP_DAYS = 90;
    static final int FEMALE_DONATION_GAP_DAYS = 120;

    private EligibilityRules() {
    }

    public static Outcome evaluate(EligibilityRequest request, List<NoteCondition> noteConditions, LocalDate today) {
        Map<EligibilityCondition, EligibilityReason> reasons = new EnumMap<>(EligibilityCondition.class);

        if (request.age() < MIN_AGE || request.age() > MAX_AGE) {
            add(reasons, EligibilityReason.of(EligibilityCondition.AGE_OUT_OF_RANGE, null, EligibilityReason.FROM_FORM));
        }
        if (request.weightKg() < MIN_WEIGHT_KG) {
            add(reasons, EligibilityReason.of(EligibilityCondition.LOW_WEIGHT, null, EligibilityReason.FROM_FORM));
        }
        if (request.lastDonationDate() != null) {
            int gap = "FEMALE".equals(request.sex()) ? FEMALE_DONATION_GAP_DAYS : MALE_DONATION_GAP_DAYS;
            waitAfter(reasons, EligibilityCondition.RECENT_DONATION, request.lastDonationDate(), Period.ofDays(gap),
                    EligibilityReason.FROM_FORM, today);
        }
        fromForm(reasons, EligibilityCondition.TATTOO_OR_PIERCING, request.tattooOrPiercingDate(), today);
        fromForm(reasons, EligibilityCondition.MINOR_SURGERY, request.minorSurgeryDate(), today);
        fromForm(reasons, EligibilityCondition.MAJOR_SURGERY, request.majorSurgeryDate(), today);
        fromForm(reasons, EligibilityCondition.FEVER_OR_ILLNESS, request.feverRecoveryDate(), today);
        fromForm(reasons, EligibilityCondition.ANTIBIOTICS, request.antibioticsLastDoseDate(), today);
        fromForm(reasons, EligibilityCondition.MALARIA, request.malariaRecoveryDate(), today);
        fromForm(reasons, EligibilityCondition.DENTAL_WORK, request.dentalWorkDate(), today);
        if (request.alcoholLast24Hours()) {
            fromForm(reasons, EligibilityCondition.ALCOHOL, today, today);
        }
        if (request.pregnantOrBreastfeeding()) {
            add(reasons, EligibilityReason.of(EligibilityCondition.PREGNANT_OR_BREASTFEEDING, null,
                    EligibilityReason.FROM_FORM));
        }

        for (NoteCondition condition : noteConditions) {
            if (!condition.code().allowedFromNote()) {
                continue;
            }
            if (condition.code().waitPeriod() == null) {
                add(reasons, EligibilityReason.of(condition.code(), null, EligibilityReason.FROM_NOTE));
            } else {
                // Without a date in the note, assume it happened today so the wait is never too short.
                LocalDate date = condition.date() == null ? today : condition.date();
                waitAfter(reasons, condition.code(), date, condition.code().waitPeriod(), EligibilityReason.FROM_NOTE,
                        today);
            }
        }

        List<EligibilityReason> list = new ArrayList<>(reasons.values());
        list.sort(Comparator.comparing(EligibilityReason::until, Comparator.nullsFirst(Comparator.reverseOrder())));
        return Outcome.of(list);
    }

    private static void fromForm(Map<EligibilityCondition, EligibilityReason> reasons, EligibilityCondition code,
                                 LocalDate date, LocalDate today) {
        if (date != null) {
            waitAfter(reasons, code, date, code.waitPeriod(), EligibilityReason.FROM_FORM, today);
        }
    }

    private static void waitAfter(Map<EligibilityCondition, EligibilityReason> reasons, EligibilityCondition code,
                                  LocalDate date, Period wait, String source, LocalDate today) {
        LocalDate happened = date.isAfter(today) ? today : date;
        LocalDate until = happened.plus(wait);
        if (until.isAfter(today)) {
            add(reasons, EligibilityReason.of(code, until, source));
        }
    }

    private static void add(Map<EligibilityCondition, EligibilityReason> reasons, EligibilityReason reason) {
        reasons.merge(reason.code(), reason, (current, next) -> {
            if (current.until() == null) return current;
            if (next.until() == null) return next;
            return next.until().isAfter(current.until()) ? next : current;
        });
    }

    /**
     * {@code deferredUntil} is the first day the donor can donate again. {@code seeDoctor} means at least one
     * reason has no fixed date, so there is no date to give.
     */
    public record Outcome(boolean eligible, LocalDate deferredUntil, boolean seeDoctor, List<EligibilityReason> reasons) {
        static Outcome of(List<EligibilityReason> reasons) {
            if (reasons.isEmpty()) {
                return new Outcome(true, null, false, List.of());
            }
            boolean seeDoctor = reasons.stream().anyMatch(reason -> reason.until() == null);
            LocalDate until = seeDoctor ? null : reasons.stream().map(EligibilityReason::until)
                    .filter(Objects::nonNull).max(LocalDate::compareTo).orElse(null);
            return new Outcome(false, until, seeDoctor, List.copyOf(reasons));
        }
    }
}
