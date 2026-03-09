package com.danil.library.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@Component
public class JwtTokenProvider {

    @Value("${security.jwt.secret}")
    private String secret;

    @Value("${security.jwt.access-ttl-seconds:900}")
    private long accessTtlSeconds;

    @Value("${security.jwt.refresh-ttl-seconds:1209600}")
    private long refreshTtlSeconds;

    private Key key;

    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /** Access токен (короткий) —  */
    public String generateAccessToken(Long userId, String username, String role) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(accessTtlSeconds);

        String jti = "acc:" + UUID.randomUUID();

        return Jwts.builder()
                .setId(jti)
                .setSubject(username)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(exp))
                .addClaims(Map.of(
                        "uid", userId,
                        "role", role,
                        "typ", "access"
                ))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /** Refresh токен (длинный) —  */
    public String generateRefreshToken(Long userId, String username, String role, Long sid) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(refreshTtlSeconds);

        String jti = "ref:" + UUID.randomUUID();

        return Jwts.builder()
                .setId(jti)
                .setSubject(username)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(exp))
                .addClaims(Map.of(
                        "uid", userId,
                        "role", role,
                        "typ", "refresh",
                        "sid", sid
                ))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims parseAndGetClaims(String token) {
        return parseAndValidate(token).getBody();
    }

    public Jws<Claims> parseAndValidate(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token);
    }

    public void assertTokenType(Claims claims, String expectedTyp) {
        String typ = claims.get("typ", String.class);
        if (!expectedTyp.equals(typ)) {
            throw new JwtException("Wrong token type: expected=" + expectedTyp + ", actual=" + typ);
        }
    }

    public String getJti(Claims claims) {
        return claims.getId();
    }

    public Instant getExpiresAt(Claims claims) {
        return claims.getExpiration().toInstant();
    }
}
