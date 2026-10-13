package com.gutlog.domain.record.medication.dto.response;

import com.gutlog.domain.record.medication.entity.ScheduleType;
import com.gutlog.domain.record.medication.entity.UserMedicine;

import java.math.BigDecimal;

public record UserMedicineResponse(
		Long id,
		MedicineResponse medicine,
		ScheduleType scheduleType,
		Integer dailyDoseCount,
		Integer intervalDays,
		BigDecimal doseAmount,
		String doseMemo
) {
	public static UserMedicineResponse fromEntity(UserMedicine userMedicine) {
		return new UserMedicineResponse(
				userMedicine.getId(),
				MedicineResponse.fromEntity(userMedicine.getMedicine()),
				userMedicine.getScheduleType(),
				userMedicine.getDailyDoseCount(),
				userMedicine.getIntervalDays(),
				userMedicine.getDoseAmount(),
				userMedicine.getDoseMemo()
		);
	}
}
