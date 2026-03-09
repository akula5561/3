package com.danil.library.dto;

public record TokenPairResponse(
        String accessToken,
        String refreshToken
) {}
