package com.gutlog.domain.record.meal.dto.response;

import com.gutlog.domain.record.meal.entity.Food;

public record FoodResponse(
		String name
) {
	public static FoodResponse fromName(String name){
		return new FoodResponse(name);
	}
}
