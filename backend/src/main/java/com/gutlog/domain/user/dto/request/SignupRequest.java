package com.gutlog.domain.user.dto.request;

import com.gutlog.domain.user.entity.IbdType;
import com.gutlog.domain.user.entity.Role;
import com.gutlog.domain.user.entity.User;
import jakarta.validation.constraints.*;

public record SignupRequest(
        @NotBlank(message = "아이디는 필수입니다.")
        @Pattern(regexp = "^[a-z0-9]{4,16}$", message = "아이디는 영문 소문자와 숫자로 4~16자 입력해주세요.")
        String username,
        @NotBlank(message = "비밀번호는 필수입니다.")
        @Size(min=8, max=20, message = "비밀번호는 8자 이상 20자 이하로 입력해주세요.")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "비밀번호는 영문과 숫자를 모두 포함해야 합니다.")
        String password,
        @NotBlank(message = "닉네임은 필수입니다.")
        @Size(max=8, message = "닉네임은 8자 이하로 입력해주세요.")
        String nickname,
        @NotNull(message = "유저 타입 선택은 필수입니다.")
        Role role,
        IbdType ibdType
) {
        public User toEntity(String encodedPassword){
                return User.create(username, encodedPassword, nickname, role, ibdType);
        }

        @AssertTrue(message = "환자는 질환 종류를 선택해야 합니다.")
        public boolean isIbdTypeValid() {
            return role != Role.PATIENT || ibdType != null;
        }
}
