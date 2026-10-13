package com.bloodconnect.request;

import org.springframework.data.jpa.repository.JpaRepository;


import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BloodRequestRepository extends JpaRepository<BloodRequest, UUID> {
    Optional<BloodRequest> findByIdAndRequesterId(UUID id, UUID requesterId);

    List<BloodRequest> findAllByStatus(RequestStatus status);

    List<BloodRequest> findAllByStatusOrderByCreatedAtDesc(RequestStatus status);

    long countByStatus(RequestStatus status);
}