package com.example.shop.controller;

import com.example.shop.dto.TicketResponse;
import com.example.shop.entity.License;
import com.example.shop.repository.UserRepository;
import com.example.shop.service.LicenseService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/licenses")
@RequiredArgsConstructor
public class LicenseController {

    private final LicenseService licenseService;
    private final UserRepository userRepository;

    // 1. Создание лицензии (только ADMIN)
    @PostMapping
    public ResponseEntity<License> createLicense(@RequestBody CreateLicenseRequest request,
                                                 @AuthenticationPrincipal UserDetails userDetails) {
        Long adminId = getUserId(userDetails);
        License license = licenseService.createLicense(
                request.getProductId(),
                request.getTypeId(),
                request.getOwnerId(),
                request.getDeviceCount(),
                request.getDescription(),
                adminId
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(license);
    }

    // 2. Активация лицензии
    @PostMapping("/activate")
    public ResponseEntity<TicketResponse> activate(@RequestBody ActivateRequest request,
                                                   @AuthenticationPrincipal UserDetails userDetails) {
        TicketResponse response = licenseService.activateLicense(
                request.getActivationKey(),
                request.getDeviceMac(),
                request.getDeviceName(),
                getUserId(userDetails)
        );
        return ResponseEntity.ok(response);
    }

    // 3. Продление лицензии
    @PostMapping("/renew")
    public ResponseEntity<TicketResponse> renew(@RequestBody RenewRequest request,
                                                @AuthenticationPrincipal UserDetails userDetails) {
        TicketResponse response = licenseService.renewLicense(
                request.getActivationKey(),
                getUserId(userDetails)
        );
        return ResponseEntity.ok(response);
    }

    // 4. Проверка лицензии
    @GetMapping("/check")
    public ResponseEntity<TicketResponse> check(@RequestParam String deviceMac,
                                                @RequestParam Long productId,
                                                @AuthenticationPrincipal UserDetails userDetails) {
        TicketResponse response = licenseService.checkLicense(
                deviceMac,
                getUserId(userDetails),
                productId
        );
        return ResponseEntity.ok(response);
    }

    private Long getUserId(UserDetails userDetails) {
        return userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"))
                .getId();
    }

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
