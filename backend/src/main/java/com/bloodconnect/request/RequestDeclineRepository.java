package com.bloodconnect.request;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RequestDeclineRepository extends JpaRepository<RequestDecline, UUID> {
    boolean existsByRequestIdAndDonorId(UUID requestId, UUID donorId);
    List<RequestDecline> findAllByDonorId(UUID donorId);
    List<RequestDecline> findAllByRequestIdOrderByCreatedAtDesc(UUID requestId);
}
