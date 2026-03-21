package com.example.autoservice.model;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "device")
public class Device {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    private String name;

    @Column(name = "mac_address", unique = true)
    private String macAddress; // Важно для репозитория (findByMacAddress)

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    public Device() {}
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getMacAddress() { return macAddress; }
    public void setMacAddress(String macAddress) { this.macAddress = macAddress; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}