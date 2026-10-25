package com.gutlog.domain.record.medication.controller;

import com.gutlog.domain.record.medication.dto.response.MedicineResponse;
import com.gutlog.domain.record.medication.service.MedicineService;
import com.gutlog.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/medicines")
@RequiredArgsConstructor
public class MedicineController {
	private final MedicineService medicineService;

	//검색 (활성 약만)
	@GetMapping
	public ApiResponse<List<MedicineResponse>> searchMedicines(@RequestParam(required = false) String keyword) {
		return ApiResponse.success(medicineService.searchMedicines(keyword));
	}
}
