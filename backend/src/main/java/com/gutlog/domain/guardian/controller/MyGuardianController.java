package com.gutlog.domain.guardian.controller;

import com.gutlog.domain.guardian.dto.response.InviteCodeResponse;
import com.gutlog.domain.guardian.service.GuardianInviteCodeService;
import com.gutlog.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 환자 쪽 보호자 연결 API. PATIENT가 아니면 모두 404다.
 */
@RestController
@RequestMapping("/api/my-guardian")
@RequiredArgsConstructor
public class MyGuardianController {
	private final GuardianInviteCodeService inviteCodeService;

	//현재 초대 코드 (유효한 코드가 없으면 null)
	@GetMapping("/invite-code")
	public ApiResponse<InviteCodeResponse> getInviteCode(@AuthenticationPrincipal Long userId) {
		return ApiResponse.success(inviteCodeService.getInviteCode(userId));
	}

	//발급·재발급
	@PostMapping("/invite-code")
	public ApiResponse<InviteCodeResponse> issueInviteCode(@AuthenticationPrincipal Long userId) {
		return ApiResponse.success(inviteCodeService.issueInviteCode(userId));
	}
}
