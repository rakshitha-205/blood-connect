package com.bloodconnect.auth;

public final class PhoneNumbers {

    public static final String PATTERN = "^\\+?[0-9][0-9 -]{8,18}[0-9]$";
    public static final String MESSAGE = "Enter a valid phone number, e.g. 9876543210 or +919876543210.";

    private PhoneNumbers() {
    }

    public static String normalize(String phone) {
        String digits = phone.replaceAll("[^0-9]", "");
        if (phone.trim().startsWith("+")) {
            return "+" + digits;
        }
        return digits.length() == 10 ? "+91" + digits : "+" + digits;
    }

    public static String mask(String phone) {
        return phone.length() <= 4 ? phone : "•".repeat(phone.length() - 4) + phone.substring(phone.length() - 4);
    }
}
