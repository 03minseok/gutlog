package com.gutlog.domain.record.weight.controller;

import com.gutlog.domain.record.weight.dto.request.WeightRequest;
import com.gutlog.domain.record.weight.dto.response.WeightResponse;
import com.gutlog.domain.record.weight.service.WeightService;
import com.gutlog.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/record/weight")
@RequiredArgsConstructor
public class WeightController {
	private final WeightService weightService;

	//기간 조회
	@GetMapping
	public ApiResponse<List<WeightResponse>> getWeightRecordsByPeriod(@AuthenticationPrincipal Long userId, @RequestParam LocalDate from, @RequestParam LocalDate to) {
		return ApiResponse.success(weightService.getWeightRecordsByPeriod(userId, from, to));
	}

	//등록·수정 (날짜당 한 줄, 있으면 값을 바꾼다)
	@PutMapping("/{date}")
	public ApiResponse<WeightResponse> saveWeightRecord(@AuthenticationPrincipal Long userId, @PathVariable LocalDate date, @Valid @RequestBody WeightRequest request) {
		return ApiResponse.success(weightService.saveWeightRecord(userId, date, request));
	}

	//삭제
	@DeleteMapping("/{date}")
	public ApiResponse<Void> deleteWeightRecord(@AuthenticationPrincipal Long userId, @PathVariable LocalDate date) {
		weightService.deleteWeightRecord(userId, date);
		return ApiResponse.success(null);
	}
}
