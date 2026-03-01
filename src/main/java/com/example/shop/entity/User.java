package com.example.shop.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    private String email;

    @Enumerated(EnumType.STRING)
    private Role role;

    @Column(nullable = false)
    private boolean isAccountExpired = false;

    @Column(nullable = false)
    private boolean isAccountLocked = false;

    @Column(nullable = false)
    private boolean isCredentialsExpired = false;

    @Column(nullable = false)
    private boolean isDisabled = false;
}
