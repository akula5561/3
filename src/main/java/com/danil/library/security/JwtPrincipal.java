package com.danil.library.security;

import java.io.Serializable;

/**
 * Субъект после проверки access JWT: id пользователя нужен для вызовов сервиса, как на диаграммах (userId / adminId).
 */
public record JwtPrincipal(Long userId, String username, String role) implements Serializable {
}
