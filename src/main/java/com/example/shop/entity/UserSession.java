package com.example.shop.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String userEmail;       // идентификатор пользователя

    private String deviceId;        // идентификатор устройства / сессии

    @Column(length = 512)
    private String accessToken;     // JWT access-токен

    @Column(length = 512)
    private String refreshToken;    // JWT refresh-токен

    private Instant accessTokenExpiry;   // время истечения access-токена

    private Instant refreshTokenExpiry;  // время истечения refresh-токена

    @Enumerated(EnumType.STRING)
    private SessionStatus status;   // ACTIVE, USED, REVOKED
}
