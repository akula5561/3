package com.danil.library.security;

import com.danil.library.dto.LoginRequest;
import com.danil.library.dto.RefreshRequest;
import com.danil.library.dto.TokenPairResponse;
import com.danil.library.model.UserAccount;
import com.danil.library.repository.UserAccountRepository;
import com.danil.library.repository.UserSessionRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

@Service
public class AuthService {

    private final UserAccountRepository users;
    private final UserSessionRepository sessions;
    private final PasswordEncoder encoder;
    private final JwtTokenProvider jwt;

    public AuthService(
            UserAccountRepository users,
            UserSessionRepository sessions,
            PasswordEncoder encoder,
            JwtTokenProvider jwt
    ) {
        this.users = users;
        this.sessions = sessions;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    public TokenPairResponse login(LoginRequest req) {
        UserAccount user = users.findByUsername(req.username())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        if (!encoder.matches(req.password(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Bad credentials");
        }

        if (user.isDisabled() || user.isAccountLocked() || user.isAccountExpired() || user.isCredentialsExpired()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Account inactive");
        }

        Instant now = Instant.now();

        // 1) Сначала создаём сессию, чтобы получить sid
        // expiresAt заранее ставим как now + refresh TTL (точность до секунды ок)
        Instant sessionExp = now.plusSeconds(getRefreshTtlSeconds());
        UserSession session = new UserSession(user.getId(), sessionExp);
        session = sessions.save(session); // <- тут появляется session.getId() (sid)

        // 2) Теперь генерим refresh с sid
        String refresh = jwt.generateRefreshToken(user.getId(), user.getUsername(), user.getRole(), session.getId());
        Claims refreshClaims = safeParse(refresh);
        jwt.assertTokenType(refreshClaims, "refresh");

        // 3) Вытаскиваем jti и exp из токена и обновляем запись сессии
        String refreshJti = jwt.getJti(refreshClaims);
        Instant refreshExp = jwt.getExpiresAt(refreshClaims);

        session.setRefreshJti(refreshJti);
        session.setExpiresAt(refreshExp);
        session.setStatus(SessionStatus.ACTIVE);
        sessions.save(session);

        // 4) Access (без sid)
        String access = jwt.generateAccessToken(user.getId(), user.getUsername(), user.getRole());

        return new TokenPairResponse(access, refresh);
    }

    public TokenPairResponse refresh(RefreshRequest req) {
        Instant now = Instant.now();

        // 1) Валидируем refresh (подпись/exp)
        Claims claims = safeParse(req.refreshToken());
        jwt.assertTokenType(claims, "refresh");

        Long sid = claims.get("sid", Long.class);
        if (sid == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token has no sid");
        }

        String tokenJti = jwt.getJti(claims);
        Instant tokenExp = jwt.getExpiresAt(claims);

        // 2) Достаём сессию по sid
        UserSession session = sessions.findById(sid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unknown session"));

        // 3) Проверяем одноразовость и соответствие jti
        // Если refresh уже ротировали/отозвали — запрет
        if (session.getStatus() != SessionStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Refresh already used (rotated/revoked)");
        }

        // Если jti не совпал — это подмена/старый токен
        if (session.getRefreshJti() == null || !session.getRefreshJti().equals(tokenJti)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Refresh does not match active session token");
        }

        // 4) Проверяем истечение
        if (tokenExp.isBefore(now) || session.getExpiresAt().isBefore(now)) {
            session.setStatus(SessionStatus.EXPIRED);
            sessions.save(session);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh expired");
        }

        // 5) Rotation: старую сессию помечаем ROTATED
        session.setStatus(SessionStatus.ROTATED);
        session.setRotatedAt(now);
        sessions.save(session);

        // 6) Создаём новую сессию (новый sid) и новую пару токенов
        Long uid = claims.get("uid", Long.class);
        String username = claims.getSubject();
        String role = claims.get("role", String.class);

        Instant newSessionExp = now.plusSeconds(getRefreshTtlSeconds());
        UserSession newSession = new UserSession(uid, newSessionExp);
        newSession = sessions.save(newSession);

        String newRefresh = jwt.generateRefreshToken(uid, username, role, newSession.getId());
        Claims newRefreshClaims = safeParse(newRefresh);

        newSession.setRefreshJti(jwt.getJti(newRefreshClaims));
        newSession.setExpiresAt(jwt.getExpiresAt(newRefreshClaims));
        newSession.setStatus(SessionStatus.ACTIVE);
        sessions.save(newSession);

        String newAccess = jwt.generateAccessToken(uid, username, role);

        return new TokenPairResponse(newAccess, newRefresh);
    }

    private Claims safeParse(String token) {
        try {
            return jwt.parseAndGetClaims(token);
        } catch (JwtException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token");
        }
    }


    private long getRefreshTtlSeconds() {
        // ВАЖНО: поставь в application.properties то же значение, что используешь в JwtTokenProvider
        return 1209600L; // 14 дней по умолчанию
    }
}
