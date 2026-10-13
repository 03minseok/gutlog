package com.gutlog.domain.record.medication.dto.response;

import com.gutlog.domain.record.medication.entity.ScheduleType;
import com.gutlog.domain.record.medication.entity.UserMedicine;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 날짜별 복용 현황.
 * dailyTakenCount / dailyTotalCount: 그날 복용 중이던 DAILY 약의 회차만 센다. (홈 "오늘 복약 n/m")
 * INTERVAL·AS_NEEDED 약과, 그날 기록만 있고 그날 복용 중이 아니던(이미 끊은) 약은 세지 않는다.
 */
public record DailyMedicationResponse(
		LocalDate date,
		int dailyTakenCount,
		int dailyTotalCount,
		List<Item> medicines
) {
	/**
	 * stopped: 지금 끊은 상태인지 (끊은 약은 체크할 수 없다)
	 * takenDoses: 그날 체크한 회차. 하루 복용 횟수를 줄였다면 범위 밖 회차도 그대로 들어 있다.
	 * lastTakenDate / nextDueDate: INTERVAL 약만. 조회 날짜까지의 마지막 투여일과 그 다음 예정일. 기록이 없으면 null
	 */
	public record Item(
			Long userMedicineId,
			MedicineResponse medicine,
			ScheduleType scheduleType,
			Integer dailyDoseCount,
			Integer intervalDays,
			BigDecimal doseAmount,
			String doseMemo,
			boolean stopped,
			List<Integer> takenDoses,
			LocalDate lastTakenDate,
			LocalDate nextDueDate
	) {
		public static Item of(UserMedicine userMedicine, List<Integer> takenDoses, LocalDate lastTakenDate) {
			LocalDate nextDueDate = lastTakenDate == null ? null : lastTakenDate.plusDays(userMedicine.getIntervalDays());
			return new Item(
					userMedicine.getId(),
					MedicineResponse.fromEntity(userMedicine.getMedicine()),
					userMedicine.getScheduleType(),
					userMedicine.getDailyDoseCount(),
					userMedicine.getIntervalDays(),
					userMedicine.getDoseAmount(),
					userMedicine.getDoseMemo(),
					userMedicine.isDeleted(),
					takenDoses,
					lastTakenDate,
					nextDueDate
			);
		}
	}
}
