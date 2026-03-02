package com.example.shop.controller;

import com.example.shop.dto.Ticket;
import com.example.shop.signature.SigningService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/signature")
@RequiredArgsConstructor
public class SignatureController {

    private final SigningService signingService;

    /**
     * Проверяет подпись тикета.
     * POST /api/signature/verify
     * Тело: { "ticket": { ... }, "signature": "BASE64..." }
     * Ответ: { "valid": true/false }
     */
    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verify(@RequestBody VerifyRequest request) {
        boolean valid = signingService.verify(request.getTicket(), request.getSignature());
        return ResponseEntity.ok(Map.of(
                "valid", valid,
                "message", valid ? "Подпись корректна" : "Подпись недействительна"
        ));
    }

    @Data
    public static class VerifyRequest {
        private Ticket ticket;
        private String signature;
    }
}
