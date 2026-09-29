package com.gutlog.domain.user.dto.request;

public record LoginRequest(
        String username,
        String password
) {
}
