package com.gutlog.domain.record.bowel.dto.response;

import com.gutlog.domain.record.bowel.entity.BowelRecord;
import com.gutlog.domain.record.bowel.entity.BristolType;

import java.time.LocalDateTime;

public record BowelResponse(
		Long id,
		LocalDateTime bowelAt,
		boolean bleeding,
		BristolType bowelCondition,
		Integer painLevel,
		String memo
) {
	public static BowelResponse fromEntity(BowelRecord bowelRecord){
		return new BowelResponse(
				bowelRecord.getId(),
				bowelRecord.getBowelAt(),
				bowelRecord.isBleeding(),
				bowelRecord.getBowelCondition(),
				bowelRecord.getPainLevel(),
				bowelRecord.getMemo()
		);
	}
}
