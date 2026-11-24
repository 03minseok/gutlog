package com.gutlog.domain.guardian.entity;

/**
 * 기록 종류별 공개 범위. EDIT는 VIEW의 조회도 할 수 있다.
 */
public enum GuardianAccess {
	PRIVATE,
	VIEW,
	EDIT
}
