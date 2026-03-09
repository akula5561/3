package com.danil.library.repository;

import com.danil.library.security.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;

public interface UserSessionRepository extends JpaRepository<UserSession, Long> {

    Optional<UserSession> findByRefreshJti(String refreshJti);

    //
    long deleteByExpiresAtBefore(Instant now);
}
