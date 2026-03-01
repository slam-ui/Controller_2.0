package com.example.shop.repository;

import com.example.shop.entity.License;
import com.example.shop.entity.LicenseHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LicenseHistoryRepository extends JpaRepository<LicenseHistory, Long> {
    long countByLicenseAndStatus(License license, String status);
}
