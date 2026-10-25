package com.gutlog.domain.record.meal.service;

import com.gutlog.domain.record.meal.dto.response.FoodResponse;
import com.gutlog.domain.record.meal.entity.Food;
import com.gutlog.domain.record.meal.repository.FoodRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FoodService {
	private final FoodRepository foodRepository;

	public List<FoodResponse> getAllFoodsByUserId(Long userId){
		return foodRepository.findNamesOrderByUsage(userId).stream().map(FoodResponse::fromName).toList();
	}
}
