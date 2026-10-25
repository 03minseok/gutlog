package com.gutlog.domain.record.bowel.dto.response;

import com.gutlog.domain.record.bowel.entity.BowelRecord;
import com.gutlog.domain.record.bowel.entity.BristolType;

import java.time.LocalDateTime;

public record BowelResponse(
		Long id,
		LocalDateTime bowelAt,
		Boolean bleeding,
		BristolType bowelCondition,
		Integer painLevel,
		String memo
) {
	public static BowelResponse fromEntity(BowelRecord bowelRecord){
		return new BowelResponse(
				bowelRecord.getId(),
				bowelRecord.getBowelAt(),
				bowelRecord.getBleeding(),
				bowelRecord.getBowelCondition(),
				bowelRecord.getPainLevel(),
				bowelRecord.getMemo()
		);
	}
}
