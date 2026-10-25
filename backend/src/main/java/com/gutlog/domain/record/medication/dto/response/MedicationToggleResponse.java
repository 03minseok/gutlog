package com.gutlog.domain.record.medication.dto.response;

import java.time.LocalDate;

/** 토글 결과. taken: 바뀐 뒤의 상태 (true = 체크됨) */
public record MedicationToggleResponse(
		Long userMedicineId,
		LocalDate takenDate,
		int doseNumber,
		boolean taken
) {
}
