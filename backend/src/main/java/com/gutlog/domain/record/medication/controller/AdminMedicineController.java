package com.gutlog.domain.record.medication.controller;

import com.gutlog.domain.record.medication.dto.request.MedicineRequest;
import com.gutlog.domain.record.medication.dto.response.MedicineResponse;
import com.gutlog.domain.record.medication.service.AdminMedicineService;
import com.gutlog.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 관리자 약 관리. /api/admin/** 은 SecurityConfig에서 ADMIN만 허용한다. */
@RestController
@RequestMapping("/api/admin/medicines")
@RequiredArgsConstructor
public class AdminMedicineController {
	private final AdminMedicineService adminMedicineService;

	//전체조회 (비활성 포함)
	@GetMapping
	public ApiResponse<List<MedicineResponse>> getAllMedicines() {
		return ApiResponse.success(adminMedicineService.getAllMedicines());
	}

	//추가
	@PostMapping
	public ApiResponse<MedicineResponse> createMedicine(@Valid @RequestBody MedicineRequest request) {
		return ApiResponse.success(adminMedicineService.createMedicine(request));
	}

	//수정
	@PutMapping("/{medicineId}")
	public ApiResponse<MedicineResponse> editMedicine(@PathVariable Long medicineId, @Valid @RequestBody MedicineRequest request) {
		return ApiResponse.success(adminMedicineService.editMedicine(medicineId, request));
	}

	//비활성화
	@PatchMapping("/{medicineId}/deactivate")
	public ApiResponse<MedicineResponse> deactivateMedicine(@PathVariable Long medicineId) {
		return ApiResponse.success(adminMedicineService.deactivateMedicine(medicineId));
	}

	//재활성화
	@PatchMapping("/{medicineId}/activate")
	public ApiResponse<MedicineResponse> activateMedicine(@PathVariable Long medicineId) {
		return ApiResponse.success(adminMedicineService.activateMedicine(medicineId));
	}
}
