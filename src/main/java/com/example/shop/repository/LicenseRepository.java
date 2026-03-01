package com.example.shop.repository;

import com.example.shop.entity.License;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface LicenseRepository extends JpaRepository<License, Long> {

    Optional<License> findByCode(String code);

    // Поиск активной лицензии по устройству, пользователю и продукту (диаграмма 4)
    @Query("SELECT l FROM License l " +
            "JOIN DeviceLicense dl ON dl.license = l " +
            "WHERE dl.device.macAddress = :deviceMac " +
            "AND l.user.id = :userId " +
            "AND l.product.id = :productId " +
            "AND l.isBlocked = false " +
            "AND l.endingDate >= :now")
    Optional<License> findActiveByDeviceUserAndProduct(
            String deviceMac, Long userId, Long productId, LocalDateTime now);

    // Поиск активной лицензии только по пользователю и продукту (без устройства)
    @Query("SELECT l FROM License l WHERE l.user.id = :userId " +
            "AND l.product.id = :productId " +
            "AND l.isBlocked = false " +
            "AND l.endingDate >= :now")
    Optional<License> findActiveByUserAndProduct(Long userId, Long productId, LocalDateTime now);
}
