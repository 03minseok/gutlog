package com.gutlog.domain.record.medication.dto.response;

import com.gutlog.domain.record.medication.entity.MedicationLog;

import java.time.LocalDate;

/** 기간 조회의 복용 기록 한 건 */
public record MedicationLogResponse(
		Long id,
		Long userMedicineId,
		MedicineResponse medicine,
		LocalDate takenDate,
		int doseNumber
) {
	public static MedicationLogResponse fromEntity(MedicationLog log) {
		return new MedicationLogResponse(
				log.getId(),
				log.getUserMedicine().getId(),
				MedicineResponse.fromEntity(log.getUserMedicine().getMedicine()),
				log.getTakenDate(),
				log.getDoseNumber()
		);
	}
}
