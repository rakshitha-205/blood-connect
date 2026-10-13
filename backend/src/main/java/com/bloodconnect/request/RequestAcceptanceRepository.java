package com.bloodconnect.request;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RequestAcceptanceRepository extends JpaRepository<RequestAcceptance, UUID> {

    boolean existsByRequestIdAndDonorId(UUID requestId, UUID donorId);

    boolean existsByRequestIdAndDonorUserId(UUID requestId, UUID donorUserId);

    List<RequestAcceptance> findAllByDonorIdOrderByCreatedAtDesc(UUID donorId);

    List<RequestAcceptance> findAllByRequestIdOrderByCreatedAtAsc(UUID requestId);
}
