package com.bloodconnect.bloodbank;

import com.bloodconnect.user.AppUser;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "blood_banks")
public class BloodBank {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private AppUser user;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 30)
    private String phone;

    @Column(nullable = false, length = 160)
    private String location;

    private Double latitude;

    private Double longitude;

    @Column(name = "open_hours", length = 80)
    private String openHours;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "blood_bank_stock", joinColumns = @JoinColumn(name = "bank_id"))
    @MapKeyColumn(name = "blood_group", length = 3)
    @Column(name = "units", nullable = false)
    private Map<String, Integer> stock = new HashMap<>();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected BloodBank() {
    }

    public BloodBank(AppUser user) {
        this.user = user;
    }

    public void update(String name, String phone, String location, Double latitude, Double longitude, String openHours) {
        this.name = name;
        this.phone = phone;
        this.location = location;
        this.latitude = latitude;
        this.longitude = longitude;
        this.openHours = openHours;
        this.updatedAt = Instant.now();
    }

    public void replaceStock(Map<String, Integer> units) {
        stock.clear();
        stock.putAll(units);
        updatedAt = Instant.now();
    }

    @PrePersist
    @PreUpdate
    void touch() {
        if (updatedAt == null) {
            updatedAt = Instant.now();
        }
    }

    public UUID getId() { return id; }
    public AppUser getUser() { return user; }
    public String getName() { return name; }
    public String getPhone() { return phone; }
    public String getLocation() { return location; }
    public Double getLatitude() { return latitude; }
    public Double getLongitude() { return longitude; }
    public String getOpenHours() { return openHours; }
    public Map<String, Integer> getStock() { return stock; }
    public Instant getUpdatedAt() { return updatedAt; }
}
