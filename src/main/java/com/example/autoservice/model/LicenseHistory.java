package com.example.autoservice.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "license_history")
public class LicenseHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "license_id")
    private License license;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    private String status; // "CREATED", "ACTIVATED", "RENEWED"
    private Instant change_date;
    private String description;

    public LicenseHistory() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public License getLicense() { return license; }
    public void setLicense(License license) { this.license = license; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getChange_date() { return change_date; }
    public void setChange_date(Instant change_date) { this.change_date = change_date; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}