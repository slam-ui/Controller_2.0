package com.example.shop.controller;

import com.example.shop.dto.Ticket;
import com.example.shop.entity.License;
import com.example.shop.service.LicenseService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/licenses")
@RequiredArgsConstructor
public class LicenseController {

    private final LicenseService licenseService;

    // 1. Создание лицензии (только ADMIN)
    // POST /api/licenses
    @PostMapping
    public ResponseEntity<License> createLicense(@RequestBody CreateLicenseRequest request,
                                                  @AuthenticationPrincipal UserDetails userDetails) {
        // adminId получаем через userDetails (упрощённо берём из репозитория)
        License license = licenseService.createLicense(
                request.getProductId(),
                request.getTypeId(),
                request.getOwnerId(),
                request.getDeviceCount(),
                request.getDescription(),
                1L // TODO: заменить на реальный adminId из контекста
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(license);
    }

    // 2. Активация лицензии
    // POST /api/licenses/activate
    @PostMapping("/activate")
    public ResponseEntity<Ticket> activate(@RequestBody ActivateRequest request,
                                            @AuthenticationPrincipal UserDetails userDetails) {
        Ticket ticket = licenseService.activateLicense(
                request.getActivationKey(),
                request.getDeviceMac(),
                request.getDeviceName(),
                getUserId(userDetails)
        );
        return ResponseEntity.ok(ticket);
    }

    // 3. Продление лицензии
    // POST /api/licenses/renew
    @PostMapping("/renew")
    public ResponseEntity<Ticket> renew(@RequestBody RenewRequest request,
                                         @AuthenticationPrincipal UserDetails userDetails) {
        Ticket ticket = licenseService.renewLicense(
                request.getActivationKey(),
                getUserId(userDetails)
        );
        return ResponseEntity.ok(ticket);
    }

    // 4. Проверка лицензии
    // GET /api/licenses/check?deviceMac=XX:XX&productId=1
    @GetMapping("/check")
    public ResponseEntity<Ticket> check(@RequestParam String deviceMac,
                                         @RequestParam Long productId,
                                         @AuthenticationPrincipal UserDetails userDetails) {
        Ticket ticket = licenseService.checkLicense(
                deviceMac,
                getUserId(userDetails),
                productId
        );
        return ResponseEntity.ok(ticket);
    }

    // Вспомогательный метод — получаем userId из username
    private Long getUserId(UserDetails userDetails) {
        // Упрощённо: передаём username, сервис сам найдёт по нему
        // В реальности лучше хранить id в JWT claims
        return 1L; // TODO: получить из JWT
    }

    // DTO классы запросов
    @Data
    public static class CreateLicenseRequest {
        private Long productId;
        private Long typeId;
        private Long ownerId;
        private Integer deviceCount;
        private String description;
    }

    @Data
    public static class ActivateRequest {
        private String activationKey;
        private String deviceMac;
        private String deviceName;
    }

    @Data
    public static class RenewRequest {
        private String activationKey;
    }
}
