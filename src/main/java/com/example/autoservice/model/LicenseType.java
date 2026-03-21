package com.example.autoservice.model;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "license_type")
public class LicenseType {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    private String name;
    private Integer default_duration_in_days;
    private String description;

    public LicenseType() {}
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getDefault_duration_in_days() { return default_duration_in_days; }
    public void setDefault_duration_in_days(Integer d) { this.default_duration_in_days = d; }
}