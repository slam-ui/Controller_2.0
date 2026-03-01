package com.example.shop.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "license_types")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LicenseType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(name = "default_duration", nullable = false)
    private Integer defaultDuration;

    private String description;
}