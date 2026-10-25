package com.gutlog.domain.record.medication.dto.request;

import com.gutlog.domain.record.medication.entity.ScheduleType;
import jakarta.validation.constraints.AssertTrue;

/**
 * 복용 약 등록·수정 요청이 공통으로 따르는 복용 방식 규칙.
 * DAILY는 하루 복용 횟수만, INTERVAL은 투여 간격만 받고, AS_NEEDED는 둘 다 받지 않는다.
 * 맞지 않는 값이 오면 무시하지 않고 400으로 거절한다.
 * (scheduleType이 없을 때는 @NotNull 메시지 하나만 나오도록 여기서는 통과시킨다)
 */
public interface DoseScheduleRequest {
	ScheduleType scheduleType();

	Integer dailyDoseCount();

	Integer intervalDays();

	@AssertTrue(message = "하루 복용 횟수를 입력해주세요.")
	default boolean isDailyDoseCountPresent() {
		return scheduleType() != ScheduleType.DAILY || dailyDoseCount() != null;
	}

	@AssertTrue(message = "투여 간격을 입력해주세요.")
	default boolean isIntervalDaysPresent() {
		return scheduleType() != ScheduleType.INTERVAL || intervalDays() != null;
	}

	@AssertTrue(message = "하루 복용 횟수는 매일 먹는 약에만 입력할 수 있습니다.")
	default boolean isDailyDoseCountAllowed() {
		return dailyDoseCount() == null || scheduleType() == null || scheduleType().usesDailyDoseCount();
	}

	@AssertTrue(message = "투여 간격은 정해진 간격으로 맞는 약에만 입력할 수 있습니다.")
	default boolean isIntervalDaysAllowed() {
		return intervalDays() == null || scheduleType() == null || scheduleType().usesIntervalDays();
	}
}
