package com.bloodconnect.review;

public record RatingSummary(Double average, long count) {

    public static final RatingSummary NONE = new RatingSummary(null, 0);

    public static RatingSummary of(Double average, long count) {
        return count == 0 ? NONE : new RatingSummary(Math.round(average * 10) / 10.0, count);
    }
}
