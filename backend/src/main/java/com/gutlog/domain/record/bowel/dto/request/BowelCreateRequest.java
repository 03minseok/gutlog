package com.gutlog.domain.record.bowel.dto.request;

import com.gutlog.domain.record.bowel.entity.BowelRecord;
import com.gutlog.domain.record.bowel.entity.BristolType;
import com.gutlog.domain.user.entity.User;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;

public record BowelCreateRequest(
		@NotNull(message = "배변 시간은 필수입니다.")
		LocalDateTime bowelAt,

		@NotNull(message = "혈변 여부는 필수입니다.")
		Boolean bleeding,

		@NotNull(message = "배변 상태는 필수입니다.")
		BristolType bowelCondition,

		@NotNull(message = "통증 정도는 필수입니다.")
		@Min(value = 0, message = "통증은 0 이상이어야 합니다.")
		@Max(value = 10, message = "통증은 10 이하여야 합니다.")
		Integer painLevel,

		@Size(max = 500, message = "메모는 최대 500자까지 입력 가능합니다.")
		String memo
) {
		public BowelRecord toEntity(User user){
			return BowelRecord.create(user, bowelAt, bleeding, bowelCondition, painLevel, memo);
		}
	}
