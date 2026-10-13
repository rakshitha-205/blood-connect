package com.bloodconnect.bloodbank;

import com.bloodconnect.user.AppUser;

import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/blood-banks")
public class BloodBankController {

    private final BloodBankService bankService;

    public BloodBankController(BloodBankService bankService) {
        this.bankService = bankService;
    }

    @GetMapping
    public List<BloodBankResponse> search(@RequestParam(required = false) String bloodGroup,
                                          @RequestParam(required = false) Double latitude,
                                          @RequestParam(required = false) Double longitude) {
        return bankService.search(bloodGroup == null || bloodGroup.isBlank() ? null : bloodGroup.trim(),
                latitude, longitude);
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('BLOOD_BANK')")
    public BloodBankResponse mine(@AuthenticationPrincipal AppUser user) {
        return bankService.mine(user);
    }

    @PutMapping("/me")
    @PreAuthorize("hasRole('BLOOD_BANK')")
    public BloodBankResponse saveProfile(@AuthenticationPrincipal AppUser user,
                                         @Valid @RequestBody BloodBankProfileRequest request) {
        return bankService.saveProfile(user, request);
    }

    @PutMapping("/me/stock")
    @PreAuthorize("hasRole('BLOOD_BANK')")
    public BloodBankResponse updateStock(@AuthenticationPrincipal AppUser user,
                                         @Valid @RequestBody StockUpdateRequest request) {
        return bankService.updateStock(user, request.stock());
    }
}
