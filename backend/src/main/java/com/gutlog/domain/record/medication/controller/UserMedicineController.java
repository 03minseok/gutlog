package com.gutlog.domain.record.medication.controller;

import com.gutlog.domain.record.medication.dto.request.UserMedicineCreateRequest;
import com.gutlog.domain.record.medication.dto.request.UserMedicineUpdateRequest;
import com.gutlog.domain.record.medication.dto.response.UserMedicineResponse;
import com.gutlog.domain.record.medication.service.UserMedicineService;
import com.gutlog.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/my-medicines")
@RequiredArgsConstructor
public class UserMedicineController {
	private final UserMedicineService userMedicineService;

	//목록
	@GetMapping
	public ApiResponse<List<UserMedicineResponse>> getMyMedicines(@AuthenticationPrincipal Long userId) {
		return ApiResponse.success(userMedicineService.getMyMedicines(userId));
	}

	//등록
	@PostMapping
	public ApiResponse<UserMedicineResponse> createUserMedicine(@AuthenticationPrincipal Long userId, @Valid @RequestBody UserMedicineCreateRequest request) {
		return ApiResponse.success(userMedicineService.createUserMedicine(userId, request));
	}

	//수정
	@PutMapping("/{userMedicineId}")
	public ApiResponse<UserMedicineResponse> editUserMedicine(@AuthenticationPrincipal Long userId, @PathVariable Long userMedicineId, @Valid @RequestBody UserMedicineUpdateRequest request) {
		return ApiResponse.success(userMedicineService.editUserMedicine(userId, userMedicineId, request));
	}

	//삭제 (소프트)
	@DeleteMapping("/{userMedicineId}")
	public ApiResponse<Void> deleteUserMedicine(@AuthenticationPrincipal Long userId, @PathVariable Long userMedicineId) {
		userMedicineService.deleteUserMedicine(userId, userMedicineId);
		return ApiResponse.success(null);
	}
}
