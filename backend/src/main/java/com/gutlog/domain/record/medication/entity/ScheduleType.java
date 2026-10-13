package com.gutlog.domain.record.medication.entity;

/**
 * 복용 방식.
 * DAILY: 매일 하루 n회 (회차 1~n)
 * INTERVAL: n일마다 1회 (예: 휴미라 14일, 램시마 56일). 회차는 항상 1
 * AS_NEEDED: 필요할 때. 회차는 항상 1
 */
public enum ScheduleType {
	DAILY,
	INTERVAL,
	AS_NEEDED;

	public boolean usesDailyDoseCount() {
		return this == DAILY;
	}

	public boolean usesIntervalDays() {
		return this == INTERVAL;
	}
}
