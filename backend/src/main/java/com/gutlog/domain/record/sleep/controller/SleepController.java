package com.gutlog.domain.record.sleep.controller;

import com.gutlog.domain.record.sleep.dto.request.SleepRequest;
import com.gutlog.domain.record.sleep.dto.response.SleepResponse;
import com.gutlog.domain.record.sleep.service.SleepService;
import com.gutlog.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/record/sleep")
@RequiredArgsConstructor
public class SleepController {
	private final SleepService sleepService;

	//기간 조회
	@GetMapping
	public ApiResponse<List<SleepResponse>> getSleepRecordsByPeriod(@AuthenticationPrincipal Long userId, @RequestParam LocalDate from, @RequestParam LocalDate to) {
		return ApiResponse.success(sleepService.getSleepRecordsByPeriod(userId, from, to));
	}

	//등록·수정 (날짜당 한 줄, 있으면 값을 바꾼다. 날짜는 일어난 날)
	@PutMapping("/{date}")
	public ApiResponse<SleepResponse> saveSleepRecord(@AuthenticationPrincipal Long userId, @PathVariable LocalDate date, @Valid @RequestBody SleepRequest request) {
		return ApiResponse.success(sleepService.saveSleepRecord(userId, date, request));
	}

	//삭제
	@DeleteMapping("/{date}")
	public ApiResponse<Void> deleteSleepRecord(@AuthenticationPrincipal Long userId, @PathVariable LocalDate date) {
		sleepService.deleteSleepRecord(userId, date);
		return ApiResponse.success(null);
	}
}
