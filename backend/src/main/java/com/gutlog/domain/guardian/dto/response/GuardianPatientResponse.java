package com.gutlog.domain.guardian.dto.response;

import com.gutlog.domain.guardian.entity.GuardianLink;

import java.time.LocalDateTime;

/**
 * 보호자에게 보이는 연결된 환자. 환자 정보는 닉네임만 보인다. (design.md 3.2, Q15)
 * 환자·보호자를 LAZY로 읽으므로 트랜잭션 안에서 만든다.
 */
public record GuardianPatientResponse(
		Long patientId,
		String nickname,
		LocalDateTime connectedAt,
		GuardianAccessResponse access
) {
	public static GuardianPatientResponse fromEntity(GuardianLink link) {
		return new GuardianPatientResponse(link.getPatient().getId(), link.getPatient().getNickname(),
				link.getConnectedAt(), GuardianAccessResponse.fromEntity(link));
	}
}
