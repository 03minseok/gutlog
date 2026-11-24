package com.gutlog.domain.guardian.dto.response;

import com.gutlog.domain.guardian.entity.GuardianInviteCode;

import java.time.LocalDateTime;

public record InviteCodeResponse(
		String code,
		LocalDateTime expiresAt
) {
	public static InviteCodeResponse fromEntity(GuardianInviteCode inviteCode) {
		return new InviteCodeResponse(inviteCode.getCode(), inviteCode.getExpiresAt());
	}
}
