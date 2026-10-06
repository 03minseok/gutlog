package com.gutlog.domain.record.meal.controller;

import com.gutlog.domain.record.meal.dto.request.MealCreateRequest;
import com.gutlog.domain.record.meal.dto.response.MealResponse;
import com.gutlog.domain.record.meal.service.MealRecordService;
import com.gutlog.global.response.ApiResponse;
import jakarta.websocket.server.PathParam;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/record/meal")
@RequiredArgsConstructor
public class MealController {
	private final MealRecordService mealRecordService;

	//생성
	@PostMapping
	public ApiResponse<MealResponse> uploadMealRecord(@AuthenticationPrincipal Long userId, MealCreateRequest request){
		return ApiResponse.success(mealRecordService.uploadMealRecord(userId, request));
	}

	//조회
	@GetMapping("/{mealRecordId}")
	public ApiResponse<MealResponse> getMealRecord(@AuthenticationPrincipal Long userId, @PathVariable Long mealRecordId){
		return ApiResponse.success(mealRecordService.getMealRecord(userId, mealRecordId));
	}

	//전체조회

	//삭제

	//수정
}
