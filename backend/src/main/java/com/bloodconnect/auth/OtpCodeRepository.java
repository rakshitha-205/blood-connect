package com.bloodconnect.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OtpCodeRepository extends JpaRepository<OtpCode, UUID> {
    Optional<OtpCode> findTopByUserIdAndChannelOrderByCreatedAtDesc(UUID userId, OtpChannel channel);
    void deleteAllByUserIdAndChannel(UUID userId, OtpChannel channel);
}
