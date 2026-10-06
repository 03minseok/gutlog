package com.gutlog.domain.record.meal.controller;

import com.gutlog.domain.record.meal.dto.request.MealCreateRequest;
import com.gutlog.domain.record.meal.dto.response.MealResponse;
import com.gutlog.domain.record.meal.service.MealRecordService;
import com.gutlog.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/record/meal")
@RequiredArgsConstructor
public class MealController {
	private final MealRecordService mealRecordService;

	//생성
	@PostMapping
	public ApiResponse<MealResponse> uploadMealRecord(@AuthenticationPrincipal Long userId, @Valid @RequestBody MealCreateRequest request){
		return ApiResponse.success(mealRecordService.uploadMealRecord(userId, request));
	}

	//조회
	@GetMapping("/{mealRecordId}")
	public ApiResponse<MealResponse> getMealRecord(@AuthenticationPrincipal Long userId, @PathVariable Long mealRecordId){
		return ApiResponse.success(mealRecordService.getMealRecord(userId, mealRecordId));
	}

	//전체조회
	@GetMapping
	public ApiResponse<List<MealResponse>> getAllMealRecordByPeriod(@AuthenticationPrincipal Long userId, @RequestParam LocalDate from, @RequestParam LocalDate to){
		return ApiResponse.success(mealRecordService.getAllMealRecordByPeriod(userId, from, to));
	}

	//삭제
	@DeleteMapping("/{mealRecordId}")
	public ApiResponse<Void> deleteMealRecord(@AuthenticationPrincipal Long userId, @PathVariable Long mealRecordId) {
		mealRecordService.deleteMealRecord(userId, mealRecordId);
		return ApiResponse.success(null);
	}

	//수정
	@PutMapping("/{mealRecordId}")
	public ApiResponse<MealResponse> editMealRecord(@AuthenticationPrincipal Long userId, @PathVariable Long mealRecordId, @Valid @RequestBody MealCreateRequest request){
		return ApiResponse.success(mealRecordService.editMealRecord(userId, mealRecordId, request));
	}
}
