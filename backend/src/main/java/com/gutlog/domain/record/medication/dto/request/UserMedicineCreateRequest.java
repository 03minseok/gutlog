package com.gutlog.domain.record.medication.dto.request;

import com.gutlog.domain.record.medication.entity.ScheduleType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

/**
 * 복용 약 등록. 복용량(doseAmount)의 단위·간격(0.5 또는 0.1)은 약의 제형에 따라 서비스에서 한 번 더 검사한다.
 */
public record UserMedicineCreateRequest(
		@NotNull(message = "약을 선택해주세요.")
		Long medicineId,

		@NotNull(message = "복용 방식은 필수입니다.")
		ScheduleType scheduleType,

		@Min(value = 1, message = "하루 복용 횟수는 1~6회입니다.")
		@Max(value = 6, message = "하루 복용 횟수는 1~6회입니다.")
		Integer dailyDoseCount,

		@Min(value = 2, message = "투여 간격은 2~180일입니다.")
		@Max(value = 180, message = "투여 간격은 2~180일입니다.")
		Integer intervalDays,

		@DecimalMin(value = "0.1", message = "복용량이 올바르지 않습니다.")
		@DecimalMax(value = "100", message = "복용량이 올바르지 않습니다.")
		@Digits(integer = 3, fraction = 1, message = "복용량은 소수점 한 자리까지 입력할 수 있습니다.")
		BigDecimal doseAmount,

		@Size(max = 20, message = "복용 메모는 20자 이하로 입력해주세요.")
		String doseMemo
) implements DoseScheduleRequest {
}
