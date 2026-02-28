package com.example.shop.repository;

import com.example.shop.entity.SessionStatus;
import com.example.shop.entity.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, UUID> {

    Optional<UserSession> findByRefreshToken(String refreshToken);

    // Удаляем просроченные сессии (для очистки БД)
    @Modifying
    @Query("DELETE FROM UserSession s WHERE s.refreshTokenExpiry < :now")
    void deleteExpiredSessions(Instant now);

    // Отзываем все активные сессии пользователя (при компрометации)
    @Modifying
    @Query("UPDATE UserSession s SET s.status = :status WHERE s.userEmail = :email AND s.status = 'ACTIVE'")
    void revokeAllUserSessions(String email, SessionStatus status);
}
