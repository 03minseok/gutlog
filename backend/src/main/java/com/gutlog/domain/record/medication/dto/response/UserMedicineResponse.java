package com.gutlog.domain.record.medication.dto.response;

import com.gutlog.domain.record.medication.entity.MedicationSchedule;
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
	public static UserMedicineResponse fromEntity(MedicationSchedule schedule) {
		UserMedicine userMedicine = schedule.getUserMedicine();
		return new UserMedicineResponse(
				userMedicine.getId(),
				MedicineResponse.fromEntity(userMedicine.getMedicine()),
				schedule.getScheduleType(),
				schedule.getDailyDoseCount(),
				schedule.getIntervalDays(),
				schedule.getDoseAmount(),
				schedule.getDoseMemo()
		);
	}
}
