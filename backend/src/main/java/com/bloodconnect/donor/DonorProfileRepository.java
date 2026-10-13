package com.bloodconnect.donor;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DonorProfileRepository extends JpaRepository<DonorProfile, UUID> {
    Optional<DonorProfile> findByUserId(UUID userId);
    List<DonorProfile> findAllByAvailableTrue();
}