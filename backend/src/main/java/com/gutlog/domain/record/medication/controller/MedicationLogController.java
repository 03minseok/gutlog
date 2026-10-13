package com.gutlog.domain.record.medication.controller;

import com.gutlog.domain.record.medication.dto.response.DailyMedicationResponse;
import com.gutlog.domain.record.medication.dto.response.MedicationLogResponse;
import com.gutlog.domain.record.medication.dto.response.MedicationToggleResponse;
import com.gutlog.domain.record.medication.service.MedicationLogService;
import com.gutlog.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/record/medication")
@RequiredArgsConstructor
public class MedicationLogController {
	private final MedicationLogService medicationLogService;

	//날짜별 조회
	@GetMapping("/daily")
	public ApiResponse<DailyMedicationResponse> getDailyMedication(@AuthenticationPrincipal Long userId, @RequestParam LocalDate date) {
		return ApiResponse.success(medicationLogService.getDailyMedication(userId, date));
	}

	//기간 조회
	@GetMapping
	public ApiResponse<List<MedicationLogResponse>> getMedicationLogsByPeriod(@AuthenticationPrincipal Long userId, @RequestParam LocalDate from, @RequestParam LocalDate to) {
		return ApiResponse.success(medicationLogService.getMedicationLogsByPeriod(userId, from, to));
	}

	//체크 토글
	@PostMapping("/{userMedicineId}/{takenDate}/{doseNumber}/toggle")
	public ApiResponse<MedicationToggleResponse> toggleMedicationLog(@AuthenticationPrincipal Long userId, @PathVariable Long userMedicineId,
																	 @PathVariable LocalDate takenDate, @PathVariable int doseNumber) {
		return ApiResponse.success(medicationLogService.toggleMedicationLog(userId, userMedicineId, takenDate, doseNumber));
	}
}
