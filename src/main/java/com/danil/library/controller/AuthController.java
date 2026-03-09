package com.danil.library.controller;

import com.danil.library.dto.LoginRequest;
import com.danil.library.dto.RefreshRequest;
import com.danil.library.dto.TokenPairResponse;
import com.danil.library.security.AuthService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public TokenPairResponse login(@RequestBody LoginRequest req) {
        return authService.login(req);
    }

    @PostMapping("/refresh")
    public TokenPairResponse refresh(@RequestBody RefreshRequest req) {
        return authService.refresh(req);
    }
}
