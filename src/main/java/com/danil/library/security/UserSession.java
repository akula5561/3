package com.danil.library.security;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "user_sessions")
public class UserSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="user_id", nullable = false)
    private Long userId;

    @Column(name="refresh_jti", unique = true)
    private String refreshJti;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SessionStatus status = SessionStatus.ACTIVE;

    @Column(name="expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name="created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name="rotated_at")
    private Instant rotatedAt;

    public UserSession() {}

    /** Создаём сессию ДО выпуска refresh: refreshJti можно оставить null */
    public UserSession(Long userId, Instant expiresAt) {
        this.userId = userId;
        this.expiresAt = expiresAt;
        this.status = SessionStatus.ACTIVE;
        this.createdAt = Instant.now();
    }

    // --- getters/setters ---

    public Long getId() { return id; }

    public Long getUserId() { return userId; }

    public String getRefreshJti() { return refreshJti; }
    public void setRefreshJti(String refreshJti) { this.refreshJti = refreshJti; }

    public SessionStatus getStatus() { return status; }
    public void setStatus(SessionStatus status) { this.status = status; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }

    public Instant getCreatedAt() { return createdAt; }

    public Instant getRotatedAt() { return rotatedAt; }
    public void setRotatedAt(Instant rotatedAt) { this.rotatedAt = rotatedAt; }
}
