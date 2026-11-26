package com.gutlog.domain.guardian.service;

import com.gutlog.domain.guardian.dto.response.GuardianPatientResponse;
import com.gutlog.global.exception.ErrorCode;

/**
 * 코드 입력 트랜잭션의 결과. 트랜잭션 안에서는 예외를 던지지 않고 이 결과만 돌려준다.
 * 성공이면 response가, 실패면 errorCode가 채워진다. (design.md 5.1)
 */
public record ConnectResult(
		GuardianPatientResponse response,
		ErrorCode errorCode
) {
	public static ConnectResult success(GuardianPatientResponse response) {
		return new ConnectResult(response, null);
	}

	public static ConnectResult failure(ErrorCode errorCode) {
		return new ConnectResult(null, errorCode);
	}

	public boolean isSuccess() {
		return errorCode == null;
	}
}
