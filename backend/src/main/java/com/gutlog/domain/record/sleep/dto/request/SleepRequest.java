package com.gutlog.domain.record.sleep.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record SleepRequest(
		@NotNull(message = "수면 시간은 필수입니다.")
		@Min(value = 30, message = "수면 시간은 30분 이상이어야 합니다.")
		@Max(value = 1440, message = "수면 시간은 24시간 이하여야 합니다.")
		Integer sleepMinutes
) {
	//30분 단위인지. 없거나 범위 밖이면 위 검증이 안내하므로 여기서는 통과시켜 오류 문구가 하나만 나오게 한다.
	@JsonIgnore
	@AssertTrue(message = "수면 시간은 30분 단위로 입력해 주세요.")
	public boolean isHalfHourUnit() {
		if (sleepMinutes == null || sleepMinutes < 30 || sleepMinutes > 1440) {
			return true;
		}
		return sleepMinutes % 30 == 0;
	}
}
