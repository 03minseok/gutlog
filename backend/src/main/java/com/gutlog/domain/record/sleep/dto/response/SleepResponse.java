package com.gutlog.domain.record.sleep.dto.response;

import com.gutlog.domain.record.sleep.entity.SleepRecord;

import java.time.LocalDate;

public record SleepResponse(
		LocalDate date,
		Integer sleepMinutes
) {
	public static SleepResponse fromEntity(SleepRecord sleepRecord) {
		return new SleepResponse(sleepRecord.getRecordDate(), sleepRecord.getSleepMinutes());
	}
}
