package com.gutlog.domain.record.weight.dto.response;

import com.gutlog.domain.record.weight.entity.WeightRecord;

import java.math.BigDecimal;
import java.time.LocalDate;

public record WeightResponse(
		LocalDate date,
		BigDecimal weight
) {
	public static WeightResponse fromEntity(WeightRecord weightRecord) {
		return new WeightResponse(weightRecord.getRecordDate(), weightRecord.getWeight());
	}
}
