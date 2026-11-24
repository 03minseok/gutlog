package com.gutlog.domain.guardian.entity;

import com.gutlog.domain.user.entity.User;
import com.gutlog.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 환자와 보호자의 현재 연결과 기록 종류별 공개 범위.
 * patient_id UNIQUE로 환자당 보호자 1명을 보장한다. 보호자당 환자 2명 제한은 UNIQUE로 표현할 수 없어 서비스에서 잠금으로 막는다.
 * 해제하면 이 행을 지우고 GuardianLinkHistory에 한 줄을 남긴다. 다시 연결하면 새 행이 생기므로 공개 범위는 전부 비공개로 시작한다.
 */
@Entity
@Getter
@Table(name = "guardian_link",
		uniqueConstraints = @UniqueConstraint(name = "uk_guardian_link_patient", columnNames = "patient_id"),
		indexes = @Index(name = "idx_guardian_link_guardian", columnList = "guardian_id"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GuardianLink extends BaseTimeEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "patient_id", nullable = false)
	private User patient;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "guardian_id", nullable = false)
	private User guardian;

	@Column(nullable = false)
	private LocalDateTime connectedAt;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private GuardianAccess mealAccess;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private GuardianAccess bowelAccess;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private GuardianAccess medicationAccess;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private GuardianAccess weightAccess;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private GuardianAccess sleepAccess;

	private GuardianLink(User patient, User guardian, LocalDateTime connectedAt) {
		this.patient = patient;
		this.guardian = guardian;
		this.connectedAt = connectedAt;
		this.mealAccess = GuardianAccess.PRIVATE;
		this.bowelAccess = GuardianAccess.PRIVATE;
		this.medicationAccess = GuardianAccess.PRIVATE;
		this.weightAccess = GuardianAccess.PRIVATE;
		this.sleepAccess = GuardianAccess.PRIVATE;
	}

	/** 공개 범위는 전부 비공개로 시작한다. */
	public static GuardianLink create(User patient, User guardian, LocalDateTime connectedAt) {
		return new GuardianLink(patient, guardian, connectedAt);
	}

	/** 공개 범위 전체 교체. 5개 모두 받는다. */
	public void updateAccess(GuardianAccess meal, GuardianAccess bowel, GuardianAccess medication,
			GuardianAccess weight, GuardianAccess sleep) {
		this.mealAccess = meal;
		this.bowelAccess = bowel;
		this.medicationAccess = medication;
		this.weightAccess = weight;
		this.sleepAccess = sleep;
	}
}
