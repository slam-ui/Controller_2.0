package com.example.shop.repository;

import com.example.shop.entity.Device;
import com.example.shop.entity.DeviceLicense;
import com.example.shop.entity.License;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DeviceLicenseRepository extends JpaRepository<DeviceLicense, Long> {

    List<DeviceLicense> findByLicense(License license);

    long countByLicense(License license);

    boolean existsByLicenseAndDevice(License license, Device device);
}
