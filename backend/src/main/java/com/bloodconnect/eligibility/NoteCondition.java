package com.bloodconnect.eligibility;

import java.time.LocalDate;

/** A condition the AI found in the donor's free-text note; {@code date} is null when the note gave no date. */
public record NoteCondition(EligibilityCondition code, LocalDate date) {
}
