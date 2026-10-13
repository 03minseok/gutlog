package com.gutlog.domain.record.medication.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

/**
 * 제형. 복용량 단위가 제형에 따라 정해진다. (정제 → 1정, 관장액 → 1회)
 * 복용량은 0.5 단위(반 알)로 받고, 내복액(mL)만 0.1 단위로 받는다.
 */
@Getter
@RequiredArgsConstructor
public enum DosageForm {
	TABLET("정", new BigDecimal("0.5"), new BigDecimal("20")),
	CAPSULE("캡슐", new BigDecimal("0.5"), new BigDecimal("20")),
	GRANULE("포", new BigDecimal("0.5"), new BigDecimal("20")),
	ORAL_LIQUID("mL", new BigDecimal("0.1"), new BigDecimal("100")),
	SUPPOSITORY("개", new BigDecimal("0.5"), new BigDecimal("20")),
	ENEMA("회", new BigDecimal("0.5"), new BigDecimal("20")),
	SC_INJECTION("회", new BigDecimal("0.5"), new BigDecimal("20")),
	IV_INJECTION("회", new BigDecimal("0.5"), new BigDecimal("20"));

	private final String doseUnit;
	/** 복용량 입력 단위이자 최솟값 */
	private final BigDecimal doseStep;
	private final BigDecimal maxDose;

	/** 단위(step)의 배수이고 step 이상 max 이하인지 */
	public boolean isValidDoseAmount(BigDecimal amount) {
		return amount.compareTo(doseStep) >= 0
				&& amount.compareTo(maxDose) <= 0
				&& amount.remainder(doseStep).signum() == 0;
	}
}
