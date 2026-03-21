package com.example.autoservice.model;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "product")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    private String name;
    private boolean is_blocked = false;

    public Product() {}
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public boolean is_blocked() { return is_blocked; }
    public void set_blocked(boolean blocked) { is_blocked = blocked; }
}