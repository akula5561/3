package com.danil.library.security;

import java.time.Instant;

public record RefreshTokenData(
        String token,
        String jti,
        Instant expiresAt
) {}
