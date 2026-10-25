package com.gutlog.domain.record.medication.repository;

import java.time.LocalDate;

/** MedicationLogRepository.findLastTakenDates 결과 */
public record LastTakenDate(Long userMedicineId, LocalDate takenDate) {
}
