package com.gutlog.domain.record.bowel.controller;

import com.gutlog.domain.record.bowel.dto.request.BowelCreateRequest;
import com.gutlog.domain.record.bowel.dto.response.BowelResponse;
import com.gutlog.domain.record.bowel.service.BowelService;
import com.gutlog.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/record/bowel")
@RequiredArgsConstructor
public class BowelController {
	private final BowelService bowelService;

	//생성
	@PostMapping
	public ApiResponse<BowelResponse> createBowelRecord(@AuthenticationPrincipal Long userId, @Valid @RequestBody BowelCreateRequest request){
		return ApiResponse.success(bowelService.createBowelRecord(userId, request));
	}

	//조회
	@GetMapping("/{bowelRecordId}")
	public ApiResponse<BowelResponse> getBowelRecord(@AuthenticationPrincipal Long userId, @PathVariable Long bowelRecordId){
		return ApiResponse.success(bowelService.getBowelRecord(userId, bowelRecordId));
	}

	//전체조회
	@GetMapping
	public ApiResponse<List<BowelResponse>> getAllBowelRecordsByPeriod(@AuthenticationPrincipal Long userId, @RequestParam LocalDate from, @RequestParam LocalDate to){
		return ApiResponse.success(bowelService.getAllBowelRecordsByPeriod(userId, from, to));
	}

	//수정
	@PutMapping("/{bowelRecordId}")
	public ApiResponse<BowelResponse> editBowelRecord(@AuthenticationPrincipal Long userId, @PathVariable Long bowelRecordId, @Valid @RequestBody BowelCreateRequest request){
		return ApiResponse.success(bowelService.editBowelRecord(userId, bowelRecordId, request));
	}

	//삭제
	@DeleteMapping("/{bowelRecordId}")
	public ApiResponse<Void> deleteBowelRecord(@AuthenticationPrincipal Long userId, @PathVariable Long bowelRecordId){
		bowelService.deleteBowelRecord(userId, bowelRecordId);
		return ApiResponse.success(null);
	}
}
