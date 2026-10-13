package com.bloodconnect.bloodbank;

import com.bloodconnect.auth.PhoneNumbers;
import com.bloodconnect.matching.MatchingService;
import com.bloodconnect.user.AppUser;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class BloodBankService {

    public static final List<String> BLOOD_GROUPS = List.of("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-");

    private final BloodBankRepository bankRepository;

    public BloodBankService(BloodBankRepository bankRepository) {
        this.bankRepository = bankRepository;
    }

    @Transactional(readOnly = true)
    public BloodBankResponse mine(AppUser user) {
        return bankRepository.findByUserId(user.getId())
                .map(bank -> BloodBankResponse.from(bank, null))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Add your blood bank details first."));
    }

    @Transactional
    public BloodBankResponse saveProfile(AppUser user, BloodBankProfileRequest request) {
        BloodBank bank = bankRepository.findByUserId(user.getId()).orElseGet(() -> new BloodBank(user));
        String openHours = request.openHours() == null || request.openHours().isBlank() ? null : request.openHours().trim();
        bank.update(request.name().trim(), PhoneNumbers.normalize(request.phone()), request.location().trim(),
                request.latitude(), request.longitude(), openHours);
        return BloodBankResponse.from(bankRepository.save(bank), null);
    }

    @Transactional
    public BloodBankResponse updateStock(AppUser user, Map<String, Integer> stock) {
        BloodBank bank = bankRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Save your blood bank details first."));
        stock.keySet().stream()
                .filter(group -> !BLOOD_GROUPS.contains(group))
                .findFirst()
                .ifPresent(group -> {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown blood group: " + group);
                });
        bank.replaceStock(stock.entrySet().stream()
                .filter(entry -> entry.getValue() > 0)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
        return BloodBankResponse.from(bank, null);
    }

    @Transactional(readOnly = true)
    public List<BloodBankResponse> search(String bloodGroup, Double latitude, Double longitude) {
        if (bloodGroup != null && !BLOOD_GROUPS.contains(bloodGroup)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose a valid blood group.");
        }
        return bankRepository.findAll().stream()
                .filter(bank -> bloodGroup == null || bank.getStock().getOrDefault(bloodGroup, 0) > 0)
                .map(bank -> BloodBankResponse.from(bank,
                        MatchingService.distanceKm(latitude, longitude, bank.getLatitude(), bank.getLongitude())))
                .sorted(Comparator
                        .comparing(BloodBankResponse::distanceKm, Comparator.nullsLast(Double::compareTo))
                        .thenComparing(BloodBankResponse::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }
}
