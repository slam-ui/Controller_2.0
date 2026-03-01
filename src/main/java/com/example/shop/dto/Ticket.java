package com.example.shop.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Ticket {
    private LocalDateTime serverDate;
    private Long ticketLifetime;
    private LocalDateTime activationDate;
    private LocalDateTime expirationDate;
    private Long userId;
    private String deviceId;
    private boolean isBlocked;
    private String signature;
}