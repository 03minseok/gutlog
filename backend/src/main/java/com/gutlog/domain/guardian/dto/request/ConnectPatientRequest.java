package com.gutlog.domain.guardian.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.util.Locale;

/**
 * 보호자의 초대 코드 입력. 앞뒤 공백을 지우고 대문자로 바꾼 뒤 6자리 형식을 검사한다.
 * 형식이 틀리면 서비스에 닿기 전에 400이 나므로 실패 횟수에 들어가지 않는다. (design.md 1장 C2)
 */
public record ConnectPatientRequest(
		@NotBlank(message = "초대 코드를 입력해주세요.")
		@Pattern(regexp = "^[A-Z0-9]{6}$", message = "초대 코드는 영문 대문자와 숫자 6자리입니다.")
		String code
) {
	public ConnectPatientRequest {
		if (code != null) {
			code = code.strip().toUpperCase(Locale.ROOT);
		}
	}
}
