package com.gutlog.domain.user.dto.response;

public record LoginResponse(
        String accessToken,
        UserResponse userResponse
) {
    public static LoginResponse of(String accessToken, UserResponse userResponse){
        return new LoginResponse(accessToken, userResponse);
    }
}
