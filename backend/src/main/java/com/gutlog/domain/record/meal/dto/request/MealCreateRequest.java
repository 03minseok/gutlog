package com.gutlog.domain.record.meal.dto.request;

import com.gutlog.domain.record.meal.entity.MealRecord;
import com.gutlog.domain.user.entity.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

public record MealCreateRequest(
        @NotNull(message = "식사 시간은 필수입니다.")
        LocalDateTime eatenAt,
        @NotEmpty(message = "음식을 입력해주세요.")
        @Size(max = 20, message = "음식은 최대 20개까지 입력 가능합니다.")
        List<@NotBlank(message = "음식을 입력해주세요.")
        @Size(max=50, message = "음식은 최대 50자까지 입력 가능합니다.") String> foods
) {
        public MealRecord toEntity(User user) {
                return MealRecord.create(user, eatenAt);
        }
}
