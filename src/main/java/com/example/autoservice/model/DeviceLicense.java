package com.example.autoservice.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "device_license")
public class DeviceLicense {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "license_id")
    private License license;

    @ManyToOne
    @JoinColumn(name = "device_id")
    private Device device;

    private Instant activation_date;

    public DeviceLicense() {}
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public License getLicense() { return license; }
    public void setLicense(License l) { this.license = l; }
    public Device getDevice() { return device; }
    public void setDevice(Device d) { this.device = d; }
    public Instant getActivation_date() { return activation_date; }
    public void setActivation_date(Instant d) { this.activation_date = d; }
}