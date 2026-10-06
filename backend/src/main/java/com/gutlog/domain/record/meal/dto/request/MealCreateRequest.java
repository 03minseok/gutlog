package com.gutlog.domain.record.meal.dto.request;

import com.gutlog.domain.record.meal.entity.Food;
import com.gutlog.domain.record.meal.entity.MealRecord;
import com.gutlog.domain.user.entity.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

public record MealCreateRequest(
        @NotNull(message = "식사 시간은 필수입니다.")
        LocalDateTime eatenAt,
        @NotEmpty(message = "음식을 입력해주세요.")
        @Size(max = 20, message = "음식은 최대 20개까지 입력 가능합니다.")
        List<@NotBlank(message = "음식을 입력해주세요.")
        @Size(max=50, message = "음식은 최대 50자까지 입력 가능합니다.") String> foods
) {
        public MealCreateRequest(LocalDateTime eatenAt, List<String> foods) {
                if(foods!=null){
                        foods = foods.stream()
                            .filter(Objects::nonNull)
                            .map(Food::normalizeName)
                            .distinct()
                            .toList();
                }
                this.eatenAt = eatenAt;
                this.foods = foods;
        }

        public MealRecord toEntity(User user) {
                return MealRecord.create(user, eatenAt);
        }
}
