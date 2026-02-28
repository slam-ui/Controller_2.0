package com.example.shop.service;

import com.example.shop.dto.LoginRequest;
import com.example.shop.dto.TokenResponse;
import com.example.shop.entity.Role;
import com.example.shop.entity.SessionStatus;
import com.example.shop.entity.User;
import com.example.shop.entity.UserSession;
import com.example.shop.repository.UserRepository;
import com.example.shop.repository.UserSessionRepository;
import com.example.shop.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final UserSessionRepository userSessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public User register(String username, String password, String email) {
        if (userRepository.findByUsername(username).isPresent()) {
            throw new RuntimeException("Пользователь с таким именем уже существует");
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setEmail(email);
        user.setRole(Role.USER);
        return userRepository.save(user);
    }

    @Transactional
    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Неверный пароль");
        }

        return createAndSaveTokenPair(user, null);
    }

    @Transactional
    public TokenResponse refreshToken(String refreshToken) {
        // 1. Ищем сессию по refresh-токену
        UserSession session = userSessionRepository.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new RuntimeException("Сессия не найдена. Требуется повторный вход."));

        // 2. Обнаружение повторного использования (reuse detection)
        if (session.getStatus() == SessionStatus.USED || session.getStatus() == SessionStatus.REVOKED) {
            // Токен уже был использован — возможная атака! Отзываем все сессии пользователя.
            userSessionRepository.revokeAllUserSessions(session.getUserEmail(), SessionStatus.REVOKED);
            session.setStatus(SessionStatus.REVOKED);
            userSessionRepository.save(session);
            throw new RuntimeException("Refresh-токен уже был использован. Все сессии отозваны. Войдите заново.");
        }

        // 3. Проверяем срок действия
        if (session.getRefreshTokenExpiry().isBefore(Instant.now())) {
            session.setStatus(SessionStatus.REVOKED);
            userSessionRepository.save(session);
            throw new RuntimeException("Refresh-токен истёк. Требуется повторный вход.");
        }

        // 4. Помечаем старую сессию как USED (rotation)
        session.setStatus(SessionStatus.USED);
        userSessionRepository.save(session);

        // 5. Получаем пользователя и создаём новую пару токенов
        User user = userRepository.findByUsername(session.getUserEmail())
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        return createAndSaveTokenPair(user, session.getDeviceId());
    }

    @Transactional
    public void logout(String refreshToken) {
        userSessionRepository.findByRefreshToken(refreshToken).ifPresent(session -> {
            session.setStatus(SessionStatus.REVOKED);
            userSessionRepository.save(session);
        });
    }

    // === Вспомогательный метод ===

    private TokenResponse createAndSaveTokenPair(User user, String deviceId) {
        String accessToken  = jwtTokenProvider.createAccessToken(user.getUsername(), user.getRole().name());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getUsername());

        Instant now = Instant.now();

        UserSession newSession = UserSession.builder()
                .userEmail(user.getUsername())
                .deviceId(deviceId)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .accessTokenExpiry(now.plus(15, ChronoUnit.MINUTES))
                .refreshTokenExpiry(now.plus(30, ChronoUnit.DAYS))
                .status(SessionStatus.ACTIVE)
                .build();

        userSessionRepository.save(newSession);

        return new TokenResponse(accessToken, refreshToken);
    }
}
