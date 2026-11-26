package com.gutlog.domain.guardian.controller;

import com.gutlog.domain.guardian.dto.request.ConnectPatientRequest;
import com.gutlog.domain.guardian.dto.response.GuardianPatientResponse;
import com.gutlog.domain.guardian.service.GuardianConnectService;
import com.gutlog.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 보호자 쪽 연결 API. GUARDIAN이 아니면 모두 404다.
 */
@RestController
@RequestMapping("/api/guardian/patients")
@RequiredArgsConstructor
public class GuardianPatientController {
	private final GuardianConnectService connectService;

	//초대 코드로 환자와 연결
	@PostMapping
	public ApiResponse<GuardianPatientResponse> connect(@AuthenticationPrincipal Long userId,
			@Valid @RequestBody ConnectPatientRequest request) {
		return ApiResponse.success(connectService.connect(userId, request.code()));
	}
}
