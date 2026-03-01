package com.example.shop.service;

import com.example.shop.dto.Ticket;
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

@Service
@RequiredArgsConstructor
public class LicenseService {

    private final LicenseRepository licenseRepository;
    private final LicenseTypeRepository licenseTypeRepository;
    private final LicenseHistoryRepository licenseHistoryRepository;
    private final DeviceRepository deviceRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final SigningService signingService;

    // =====================================================================
    // 1. СОЗДАНИЕ ЛИЦЕНЗИИ (только Администратор)
    // =====================================================================
    @Transactional
    public License createLicense(Long productId, Long typeId, Long ownerId,
                                 Integer deviceCount, String description, Long adminId) {
        // Проверяем существование продукта
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));

        // Проверяем тип лицензии
        LicenseType type = licenseTypeRepository.findById(typeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "License type not found"));

        // Проверяем владельца
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Owner not found"));

        // Создаём лицензию
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

        // Пишем историю
        saveHistory(saved, adminId, "CREATED", "Лицензия создана администратором");

        return saved;
    }

    // =====================================================================
    // 2. АКТИВАЦИЯ ЛИЦЕНЗИИ
    // =====================================================================
    @Transactional
    public Ticket activateLicense(String activationKey, String deviceMac,
                                  String deviceName, Long userId) {
        // Ищем лицензию по коду
        License license = licenseRepository.findByCode(activationKey)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "License not found"));

        // Проверяем: если лицензия уже активирована другим пользователем — 403
        if (license.getFirstActivationDate() != null
                && !license.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "License owned by another user");
        }

        // Ищем или создаём устройство
        Device device = deviceRepository.findByMacAddress(deviceMac)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
                    Device d = new Device();
                    d.setMacAddress(deviceMac);
                    d.setName(deviceName != null ? deviceName : deviceMac);
                    d.setUser(user);
                    return deviceRepository.save(d);
                });

        if (license.getFirstActivationDate() == null) {
            // === ПЕРВАЯ АКТИВАЦИЯ ===
            license.setFirstActivationDate(LocalDateTime.now());
            license.setEndingDate(LocalDateTime.now()
                    .plusDays(license.getType().getDefaultDuration()));
            licenseRepository.save(license);
            saveHistory(license, userId, "ACTIVATED", "Первая активация на устройстве " + deviceMac);
        } else {
            // === ПОВТОРНАЯ АКТИВАЦИЯ (новое устройство) ===
            // Проверяем лимит устройств
            long deviceCount = licenseHistoryRepository.countByLicenseAndStatus(license, "ACTIVATED");
            if (deviceCount >= license.getDeviceCount()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Device limit reached");
            }
            saveHistory(license, userId, "ACTIVATED", "Активация на устройстве " + deviceMac);
        }

        return buildTicket(license, userId, deviceMac);
    }

    // =====================================================================
    // 3. ПРОДЛЕНИЕ ЛИЦЕНЗИИ
    // =====================================================================
    @Transactional
    public Ticket renewLicense(String activationKey, Long userId) {
        License license = licenseRepository.findByCode(activationKey)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "License not found"));

        // Проверяем возможность продления:
        // лицензия не активирована ИЛИ истекает в течение 7 дней
        boolean notActivated = license.getEndingDate() == null;
        boolean expiresSoon = license.getEndingDate() != null
                && license.getEndingDate().isBefore(LocalDateTime.now().plusDays(7));

        if (!notActivated && !expiresSoon) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "License cannot be renewed yet (expires in more than 7 days)");
        }

        // Продлеваем
        LocalDateTime base = (license.getEndingDate() != null)
                ? license.getEndingDate()
                : LocalDateTime.now();
        license.setEndingDate(base.plusDays(license.getType().getDefaultDuration()));
        licenseRepository.save(license);

        saveHistory(license, userId, "RENEWED",
                "Продлена до " + license.getEndingDate());

        return buildTicket(license, userId, null);
    }

    // =====================================================================
    // 4. ПРОВЕРКА ЛИЦЕНЗИИ
    // =====================================================================
    public Ticket checkLicense(String deviceMac, Long userId, Long productId) {
        // Проверяем устройство
        Device device = deviceRepository.findByMacAddress(deviceMac)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Device not found"));

        // Ищем активную лицензию для пользователя + продукта + устройства
        License license = licenseRepository
                .findActiveByUserAndProduct(userId, productId, LocalDateTime.now())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Active license not found"));

        return buildTicket(license, userId, deviceMac);
    }

    // =====================================================================
    // Вспомогательные методы
    // =====================================================================

    private Ticket buildTicket(License license, Long userId, String deviceMac) {
        Ticket ticket = new Ticket();
        ticket.setServerDate(LocalDateTime.now());
        ticket.setTicketLifetime(3600L); // 1 час
        ticket.setActivationDate(license.getFirstActivationDate());
        ticket.setExpirationDate(license.getEndingDate());
        ticket.setUserId(userId);
        ticket.setDeviceId(deviceMac);
        ticket.setBlocked(license.isBlocked());

        // Подписываем тикет (без поля signature, чтобы не было рекурсии)
        Ticket toSign = new Ticket();
        toSign.setServerDate(ticket.getServerDate());
        toSign.setTicketLifetime(ticket.getTicketLifetime());
        toSign.setActivationDate(ticket.getActivationDate());
        toSign.setExpirationDate(ticket.getExpirationDate());
        toSign.setUserId(ticket.getUserId());
        toSign.setDeviceId(ticket.getDeviceId());
        toSign.setBlocked(ticket.isBlocked());

        ticket.setSignature(signingService.sign(toSign));
        return ticket;
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
