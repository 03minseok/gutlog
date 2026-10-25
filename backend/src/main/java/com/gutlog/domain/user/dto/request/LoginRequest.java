package com.gutlog.domain.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "아이디는 필수입니다.")
        @Size(min=3, max=20, message = "아이디는 3자 이상 20자 이하로 입력해주세요.")
        String username,
        @NotBlank(message = "비밀번호는 필수입니다.")
        @Size(min=8, max=20, message = "비밀번호는 8자 이상 20자 이하로 입력해주세요.")
        String password
) {
}
