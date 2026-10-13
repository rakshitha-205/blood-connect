package com.bloodconnect.bloodbank;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public record BloodBankResponse(UUID id, String name, String phone, String location, Double latitude,
                                Double longitude, String openHours, Map<String, Integer> stock,
                                int totalUnits, Instant updatedAt, Double distanceKm) {

    public static BloodBankResponse from(BloodBank bank, Double distanceKm) {
        Map<String, Integer> stock = new LinkedHashMap<>();
        BloodBankService.BLOOD_GROUPS.forEach(group -> stock.put(group, bank.getStock().getOrDefault(group, 0)));
        int total = stock.values().stream().mapToInt(Integer::intValue).sum();
        return new BloodBankResponse(bank.getId(), bank.getName(), bank.getPhone(), bank.getLocation(),
                bank.getLatitude(), bank.getLongitude(), bank.getOpenHours(), stock, total, bank.getUpdatedAt(),
                distanceKm);
    }
}
