package com.example.autoservice.dto;
import java.time.Instant;
import java.util.UUID;

public class Ticket {
    public Instant serverDate;
    public long ticketLifetime;
    public Instant activationDate;
    public Instant expirationDate;
    public UUID userId;
    public UUID deviceId;
    public boolean blocked;
}