package com.example.autoservice.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import java.util.List;

@Entity
@Table(name = "license")
public class License {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(unique = true)
    private String code;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne
    @JoinColumn(name = "type_id")
    private LicenseType type;

    private Instant first_activation_date;
    private Instant ending_date;
    private boolean blocked = false;
    private Integer device_count = 0;

    @ManyToOne
    @JoinColumn(name = "owner_id")
    private User owner;

    @OneToMany(mappedBy = "license", cascade = CascadeType.ALL)
    private List<DeviceLicense> deviceLicenses;

    public License() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

    public LicenseType getType() { return type; }
    public void setType(LicenseType type) { this.type = type; }

    public Instant getFirst_activation_date() { return first_activation_date; }
    public void setFirst_activation_date(Instant d) { this.first_activation_date = d; }

    public Instant getEnding_date() { return ending_date; }
    public void setEnding_date(Instant d) { this.ending_date = d; }

    public boolean isBlocked() { return blocked; }
    public void setBlocked(boolean b) { this.blocked = b; }

    public Integer getDevice_count() { return device_count; }
    public void setDevice_count(Integer device_count) { this.device_count = device_count; }

    public User getOwner() { return owner; }
    public void setOwner(User owner) { this.owner = owner; }

    public List<DeviceLicense> getDeviceLicenses() { return deviceLicenses; }
    public void setDeviceLicenses(List<DeviceLicense> deviceLicenses) { this.deviceLicenses = deviceLicenses; }
}