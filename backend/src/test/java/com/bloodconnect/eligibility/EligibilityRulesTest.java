package com.bloodconnect.eligibility;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EligibilityRulesTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 12);

    @Test
    void healthyAdultCanDonateToday() {
        EligibilityRules.Outcome outcome = evaluate(request().build(), List.of());

        assertThat(outcome.eligible()).isTrue();
        assertThat(outcome.deferredUntil()).isNull();
        assertThat(outcome.reasons()).isEmpty();
    }

    @Test
    void ageAndWeightLimitsNeedADoctorAndGiveNoDate() {
        EligibilityRules.Outcome young = evaluate(request().age(17).build(), List.of());
        EligibilityRules.Outcome light = evaluate(request().weightKg(44.5).build(), List.of());

        assertThat(young.eligible()).isFalse();
        assertThat(young.seeDoctor()).isTrue();
        assertThat(young.deferredUntil()).isNull();
        assertThat(codes(young)).containsExactly(EligibilityCondition.AGE_OUT_OF_RANGE);
        assertThat(codes(light)).containsExactly(EligibilityCondition.LOW_WEIGHT);
        assertThat(evaluate(request().age(65).weightKg(45.0).build(), List.of()).eligible()).isTrue();
    }

    @Test
    void donationGapIs90DaysForMenAnd120ForWomen() {
        EligibilityRules.Outcome man = evaluate(request().lastDonation(TODAY.minusDays(60)).build(), List.of());
        EligibilityRules.Outcome woman = evaluate(request().sex("FEMALE").lastDonation(TODAY.minusDays(100)).build(),
                List.of());

        assertThat(man.deferredUntil()).isEqualTo(TODAY.plusDays(30));
        assertThat(woman.deferredUntil()).isEqualTo(TODAY.plusDays(20));
        assertThat(evaluate(request().lastDonation(TODAY.minusDays(90)).build(), List.of()).eligible()).isTrue();
        assertThat(evaluate(request().sex("FEMALE").lastDonation(TODAY.minusDays(119)).build(), List.of())
                .deferredUntil()).isEqualTo(TODAY.plusDays(1));
    }

    @Test
    void longestWaitDecidesTheDateAndReasonsAreSortedLatestFirst() {
        EligibilityRequest request = request()
                .tattoo(TODAY.minusMonths(3))
                .antibiotics(TODAY.minusDays(2))
                .alcohol(true)
                .build();

        EligibilityRules.Outcome outcome = evaluate(request, List.of());

        assertThat(outcome.eligible()).isFalse();
        assertThat(outcome.seeDoctor()).isFalse();
        assertThat(outcome.deferredUntil()).isEqualTo(TODAY.plusMonths(9));
        assertThat(codes(outcome)).containsExactly(EligibilityCondition.TATTOO_OR_PIERCING,
                EligibilityCondition.ANTIBIOTICS, EligibilityCondition.ALCOHOL);
        assertThat(outcome.reasons().get(2).until()).isEqualTo(TODAY.plusDays(1));
    }

    @Test
    void finishedWaitsAndFutureDatesAreHandled() {
        EligibilityRules.Outcome oldTattoo = evaluate(request().tattoo(TODAY.minusMonths(13)).build(), List.of());
        EligibilityRules.Outcome futureDental = evaluate(request().dental(TODAY.plusDays(10)).build(), List.of());

        assertThat(oldTattoo.eligible()).isTrue();
        assertThat(futureDental.deferredUntil()).isEqualTo(TODAY.plusDays(3));
    }

    @Test
    void noteConditionsWithoutADateCountFromTodayAndCannotOverrideFormOnlyRules() {
        List<NoteCondition> fromNote = List.of(
                new NoteCondition(EligibilityCondition.COLD_OR_FLU, null),
                new NoteCondition(EligibilityCondition.MINOR_SURGERY, TODAY.minusMonths(2)),
                new NoteCondition(EligibilityCondition.AGE_OUT_OF_RANGE, null));

        EligibilityRules.Outcome outcome = evaluate(request().build(), fromNote);

        assertThat(outcome.deferredUntil()).isEqualTo(TODAY.plusMonths(4));
        assertThat(codes(outcome)).containsExactly(EligibilityCondition.MINOR_SURGERY, EligibilityCondition.COLD_OR_FLU);
        assertThat(outcome.reasons()).allMatch(reason -> reason.source().equals(EligibilityReason.FROM_NOTE));
    }

    @Test
    void seriousConditionOrPregnancyMeansNoDate() {
        EligibilityRules.Outcome serious = evaluate(request().tattoo(TODAY.minusMonths(1)).build(),
                List.of(new NoteCondition(EligibilityCondition.SERIOUS_CONDITION, null)));
        EligibilityRules.Outcome pregnant = evaluate(request().sex("FEMALE").pregnant(true).build(), List.of());

        assertThat(serious.seeDoctor()).isTrue();
        assertThat(serious.deferredUntil()).isNull();
        assertThat(codes(serious).get(0)).isEqualTo(EligibilityCondition.SERIOUS_CONDITION);
        assertThat(codes(pregnant)).containsExactly(EligibilityCondition.PREGNANT_OR_BREASTFEEDING);
    }

    @Test
    void sameConditionFromFormAndNoteKeepsTheLongerWait() {
        EligibilityRules.Outcome outcome = evaluate(request().antibiotics(TODAY.minusDays(5)).build(),
                List.of(new NoteCondition(EligibilityCondition.ANTIBIOTICS, null)));

        assertThat(outcome.reasons()).hasSize(1);
        assertThat(outcome.deferredUntil()).isEqualTo(TODAY.plusDays(7));
        assertThat(outcome.reasons().get(0).source()).isEqualTo(EligibilityReason.FROM_NOTE);
    }

    @Test
    void fallbackExplanationMentionsTheDate() {
        EligibilityRules.Outcome outcome = evaluate(request().lastDonation(TODAY.minusDays(60)).build(), List.of());

        assertThat(EligibilityService.fallbackExplanation(outcome)).contains("11 November 2026");
    }

    private static EligibilityRules.Outcome evaluate(EligibilityRequest request, List<NoteCondition> fromNote) {
        return EligibilityRules.evaluate(request, fromNote, TODAY);
    }

    private static List<EligibilityCondition> codes(EligibilityRules.Outcome outcome) {
        return outcome.reasons().stream().map(EligibilityReason::code).toList();
    }

    private static Builder request() {
        return new Builder();
    }

    private static final class Builder {
        private int age = 30;
        private double weightKg = 70;
        private String sex = "MALE";
        private LocalDate lastDonation;
        private LocalDate tattoo;
        private LocalDate antibiotics;
        private LocalDate dental;
        private boolean alcohol;
        private boolean pregnant;

        Builder age(int value) { age = value; return this; }
        Builder weightKg(double value) { weightKg = value; return this; }
        Builder sex(String value) { sex = value; return this; }
        Builder lastDonation(LocalDate value) { lastDonation = value; return this; }
        Builder tattoo(LocalDate value) { tattoo = value; return this; }
        Builder antibiotics(LocalDate value) { antibiotics = value; return this; }
        Builder dental(LocalDate value) { dental = value; return this; }
        Builder alcohol(boolean value) { alcohol = value; return this; }
        Builder pregnant(boolean value) { pregnant = value; return this; }

        EligibilityRequest build() {
            return new EligibilityRequest("en", age, weightKg, sex, lastDonation, tattoo, null, null, null,
                    antibiotics, null, dental, alcohol, pregnant, null);
        }
    }
}
