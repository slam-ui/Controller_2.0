package com.example.shop.controller;

import com.example.shop.entity.LicenseType;
import com.example.shop.repository.LicenseTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/license-types")
@RequiredArgsConstructor
public class LicenseTypeController {

    private final LicenseTypeRepository licenseTypeRepository;

    @PostMapping
    public ResponseEntity<LicenseType> create(@RequestBody LicenseType licenseType) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(licenseTypeRepository.save(licenseType));
    }

    @GetMapping
    public ResponseEntity<List<LicenseType>> getAll() {
        return ResponseEntity.ok(licenseTypeRepository.findAll());
    }
}