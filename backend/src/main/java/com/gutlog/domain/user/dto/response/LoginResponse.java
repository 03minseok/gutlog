package com.gutlog.domain.user.dto.response;

public record LoginResponse(
        String accessToken,
        UserResponse user
) {
    public static LoginResponse of(String accessToken, UserResponse user){
        return new LoginResponse(accessToken, user);
    }
}
