package com.gutlog.domain.guardian.dto.response;

import com.gutlog.domain.guardian.entity.GuardianAccess;
import com.gutlog.domain.guardian.entity.GuardianLink;

/**
 * 기록 종류별 공개 범위. (design.md 3장)
 */
public record GuardianAccessResponse(
		GuardianAccess meal,
		GuardianAccess bowel,
		GuardianAccess medication,
		GuardianAccess weight,
		GuardianAccess sleep
) {
	public static GuardianAccessResponse fromEntity(GuardianLink link) {
		return new GuardianAccessResponse(link.getMealAccess(), link.getBowelAccess(),
				link.getMedicationAccess(), link.getWeightAccess(), link.getSleepAccess());
	}
}
