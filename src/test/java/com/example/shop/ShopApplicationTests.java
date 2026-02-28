package com.example.shop;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

// Активируем тестовый профиль: SSL выключен, используются тестовые настройки
@SpringBootTest
@ActiveProfiles("test")
class ShopApplicationTests {

    @Test
    void contextLoads() {
        // Проверяем, что контекст Spring поднимается без ошибок
    }
}
