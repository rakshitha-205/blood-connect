package com.bloodconnect.request;

import com.bloodconnect.donor.DonorProfile;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "request_declines",
        uniqueConstraints = @UniqueConstraint(name = "uk_request_declines_request_donor",
                columnNames = {"request_id", "donor_id"}))
public class RequestDecline {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "request_id", nullable = false)
    private BloodRequest request;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "donor_id", nullable = false)
    private DonorProfile donor;

    @Column(nullable = false, length = 300)
    private String reason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected RequestDecline() {
    }

    public RequestDecline(BloodRequest request, DonorProfile donor, String reason) {
        this.request = request;
        this.donor = donor;
        this.reason = reason;
    }

    @PrePersist
    void setCreatedAt() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public UUID getId() { return id; }
    public BloodRequest getRequest() { return request; }
    public DonorProfile getDonor() { return donor; }
    public String getReason() { return reason; }
    public Instant getCreatedAt() { return createdAt; }
}
