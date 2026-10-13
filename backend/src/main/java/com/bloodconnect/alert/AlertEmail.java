package com.bloodconnect.alert;

import java.util.List;

public record AlertEmail(String to, String subject, String text) {

    /** Published inside a transaction; {@link AlertMailer} sends the batch after it commits. */
    public record Batch(List<AlertEmail> emails) {
    }
}
