package com.bloodconnect.request;

import com.bloodconnect.user.AppUser;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "blood_requests")
public class BloodRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requester_id", nullable = false)
    private AppUser requester;

    @Column(name = "patient_name", nullable = false, length = 100)
    private String patientName;

    @Column(name = "blood_group", nullable = false, length = 3)
    private String bloodGroup;

    @Column(nullable = false)
    private int units;

    @Column(nullable = false, length = 120)
    private String hospital;

    @Column(nullable = false, length = 160)
    private String location;

    private Double latitude;

    private Double longitude;

    @Column(nullable = false, length = 16)
    private String urgency;

    @Column(nullable = false, length = 30)
    private String contact;

    @Column(length = 1000)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private RequestStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected BloodRequest() {
    }

    public BloodRequest(AppUser requester, String patientName, String bloodGroup, int units, String hospital,
                        String location, Double latitude, Double longitude, String urgency,
                        String contact, String message) {
        this.requester = requester;
        this.patientName = patientName;
        this.bloodGroup = bloodGroup;
        this.units = units;
        this.hospital = hospital;
        this.location = location;
        this.latitude = latitude;
        this.longitude = longitude;
        this.urgency = urgency;
        this.contact = contact;
        this.message = message;
        this.status = RequestStatus.OPEN;
    }

    @PrePersist
    void setCreatedAt() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public void setStatus(RequestStatus status) { this.status = status; }
    public UUID getId() { return id; }
    public AppUser getRequester() { return requester; }
    public String getPatientName() { return patientName; }
    public String getBloodGroup() { return bloodGroup; }
    public int getUnits() { return units; }
    public String getHospital() { return hospital; }
    public String getLocation() { return location; }
    public Double getLatitude() { return latitude; }
    public Double getLongitude() { return longitude; }
    public String getUrgency() { return urgency; }
    public String getContact() { return contact; }
    public String getMessage() { return message; }
    public RequestStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
}