package com.gutlog.domain.guardian.dto.response;

import com.gutlog.domain.guardian.entity.GuardianLink;

import java.time.LocalDateTime;

/**
 * 환자에게 보이는 연결된 보호자. 보호자 정보는 닉네임만 보인다. (design.md 3.1, C5)
 * 보호자를 LAZY로 읽으므로 트랜잭션 안에서 만든다.
 */
public record MyGuardianResponse(
		String guardianNickname,
		LocalDateTime connectedAt,
		GuardianAccessResponse access
) {
	public static MyGuardianResponse fromEntity(GuardianLink link) {
		return new MyGuardianResponse(link.getGuardian().getNickname(), link.getConnectedAt(),
				GuardianAccessResponse.fromEntity(link));
	}
}
