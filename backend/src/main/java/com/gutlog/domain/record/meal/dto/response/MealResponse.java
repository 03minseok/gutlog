package com.gutlog.domain.record.meal.dto.response;

import com.gutlog.domain.record.meal.entity.MealRecord;

import java.time.LocalDateTime;
import java.util.List;

public record MealResponse(
        Long id,
        LocalDateTime eatenAt,
        List<String> foods
) {
    public static MealResponse of(MealRecord mealRecord, List<String> foods) {
        return new MealResponse(mealRecord.getId(), mealRecord.getEatenAt(), foods);
    }
}
