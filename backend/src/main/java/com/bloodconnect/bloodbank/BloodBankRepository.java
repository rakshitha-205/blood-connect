package com.bloodconnect.bloodbank;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BloodBankRepository extends JpaRepository<BloodBank, UUID> {

    Optional<BloodBank> findByUserId(UUID userId);
}
