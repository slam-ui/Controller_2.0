package com.example.shop.service;

import com.example.shop.dto.Ticket;
import com.example.shop.dto.TicketResponse;
import com.example.shop.entity.*;
import com.example.shop.repository.*;
import com.example.shop.signature.SigningService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LicenseService {

    private final LicenseRepository licenseRepository;
    private final LicenseTypeRepository licenseTypeRepository;
    private final LicenseHistoryRepository licenseHistoryRepository;
    private final DeviceRepository deviceRepository;
    private final DeviceLicenseRepository deviceLicenseRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final SigningService signingService;

    // =====================================================================
    // 1. СОЗДАНИЕ ЛИЦЕНЗИИ (только Администратор)
    // =====================================================================
    @Transactional
    public License createLicense(Long productId, Long typeId, Long ownerId,
                                 Integer deviceCount, String description, Long adminId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));

        LicenseType type = licenseTypeRepository.findById(typeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "License type not found"));

        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Owner not found"));

        License license = new License();
        license.setCode(generateCode());
        license.setProduct(product);
        license.setType(type);
        license.setUser(owner);
        license.setOwnerId(adminId);
        license.setDeviceCount(deviceCount != null ? deviceCount : 1);
        license.setDescription(description);
        license.setBlocked(false);

        License saved = licenseRepository.save(license);
        saveHistory(saved, adminId, "CREATED", "Лицензия создана администратором");

        return saved;
    }

    // =====================================================================
    // 2. АКТИВАЦИЯ ЛИЦЕНЗИИ
    // =====================================================================
    @Transactional
    public TicketResponse activateLicense(String activationKey, String deviceMac,
                                          String deviceName, Long userId) {
        License license = licenseRepository.findByCode(activationKey)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "License not found"));

        if (license.getFirstActivationDate() != null
                && !license.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "License owned by another user");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        Device device = deviceRepository.findByMacAddress(deviceMac)
                .orElseGet(() -> {
                    Device d = new Device();
                    d.setMacAddress(deviceMac);
                    d.setName(deviceName != null ? deviceName : deviceMac);
                    d.setUser(user);
                    return deviceRepository.save(d);
                });

        if (license.getFirstActivationDate() == null) {
            license.setUser(user);
            license.setFirstActivationDate(LocalDateTime.now());
            license.setEndingDate(LocalDateTime.now()
                    .plusDays(license.getType().getDefaultDuration()));
            licenseRepository.save(license);

            createDeviceLicense(license, device);
            saveHistory(license, userId, "ACTIVATED", "Первая активация на устройстве " + deviceMac);
        } else {
            long deviceCount = deviceLicenseRepository.countByLicense(license);
            if (deviceCount >= license.getDeviceCount()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Device limit reached");
            }

            if (!deviceLicenseRepository.existsByLicenseAndDevice(license, device)) {
                createDeviceLicense(license, device);
            }

            saveHistory(license, userId, "ACTIVATED", "Активация на устройстве " + deviceMac);
        }

        return buildTicketResponse(license, userId, deviceMac);
    }

    // =====================================================================
    // 3. ПРОДЛЕНИЕ ЛИЦЕНЗИИ
    // =====================================================================
    @Transactional
    public TicketResponse renewLicense(String activationKey, Long userId) {
        License license = licenseRepository.findByCode(activationKey)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "License not found"));

        boolean notActivated = license.getEndingDate() == null;
        boolean expiresSoon = license.getEndingDate() != null
                && license.getEndingDate().isBefore(LocalDateTime.now().plusDays(7));

        if (!notActivated && !expiresSoon) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "License cannot be renewed yet (expires in more than 7 days)");
        }

        LocalDateTime base = (license.getEndingDate() != null)
                ? license.getEndingDate()
                : LocalDateTime.now();
        license.setEndingDate(base.plusDays(license.getType().getDefaultDuration()));
        licenseRepository.save(license);

        saveHistory(license, userId, "RENEWED", "Продлена до " + license.getEndingDate());

        return buildTicketResponse(license, userId, null);
    }

    // =====================================================================
    // 4. ПРОВЕРКА ЛИЦЕНЗИИ
    // =====================================================================
    public TicketResponse checkLicense(String deviceMac, Long userId, Long productId) {
        deviceRepository.findByMacAddress(deviceMac)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Device not found"));

        List<License> licenses = licenseRepository
                .findActiveByDeviceUserAndProduct(deviceMac, userId, productId, LocalDateTime.now());

        if (licenses.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Active license not found");
        }

        License license = licenses.get(0);

        return buildTicketResponse(license, userId, deviceMac);
    }

    // =====================================================================
    // Вспомогательные методы
    // =====================================================================

    private void createDeviceLicense(License license, Device device) {
        DeviceLicense dl = new DeviceLicense();
        dl.setLicense(license);
        dl.setDevice(device);
        dl.setActivationDate(LocalDateTime.now());
        deviceLicenseRepository.save(dl);
    }

    private TicketResponse buildTicketResponse(License license, Long userId, String deviceMac) {
        Ticket ticket = new Ticket();
        ticket.setServerDate(LocalDateTime.now());
        ticket.setTicketLifetime(3600L);
        ticket.setActivationDate(license.getFirstActivationDate());
        ticket.setExpirationDate(license.getEndingDate());
        ticket.setUserId(userId);
        ticket.setDeviceId(deviceMac);
        ticket.setBlocked(license.isBlocked());

        String signature = signingService.sign(ticket);
        return new TicketResponse(ticket, signature);
    }

    private void saveHistory(License license, Long userId, String status, String description) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        LicenseHistory history = new LicenseHistory();
        history.setLicense(license);
        history.setUser(user);
        history.setStatus(status);
        history.setChangeDate(LocalDateTime.now());
        history.setDescription(description);
        licenseHistoryRepository.save(history);
    }

    private String generateCode() {
        return UUID.randomUUID().toString().toUpperCase();
    }
}
