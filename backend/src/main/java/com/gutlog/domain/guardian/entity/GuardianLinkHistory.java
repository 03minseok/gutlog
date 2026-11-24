package com.gutlog.domain.guardian.entity;

import com.gutlog.domain.user.entity.User;
import com.gutlog.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 해제된 연결의 이력. 해제할 때 한 줄을 넣는다.
 * 연결·해제 시각과 해제한 사람만 남긴다. 공개 범위는 남기지 않는다.
 */
@Entity
@Getter
@Table(name = "guardian_link_history",
		indexes = {
				@Index(name = "idx_guardian_link_history_patient", columnList = "patient_id"),
				@Index(name = "idx_guardian_link_history_guardian", columnList = "guardian_id")
		})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GuardianLinkHistory extends BaseTimeEntity {
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

	@Column(nullable = false)
	private LocalDateTime disconnectedAt;

	/** 해제한 사람. 환자 또는 보호자 본인이다. */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "disconnected_by_id", nullable = false)
	private User disconnectedBy;

	private GuardianLinkHistory(User patient, User guardian, LocalDateTime connectedAt,
			LocalDateTime disconnectedAt, User disconnectedBy) {
		this.patient = patient;
		this.guardian = guardian;
		this.connectedAt = connectedAt;
		this.disconnectedAt = disconnectedAt;
		this.disconnectedBy = disconnectedBy;
	}

	/** 해제하는 연결에서 환자·보호자·연결 시각을 옮겨 적는다. */
	public static GuardianLinkHistory of(GuardianLink link, LocalDateTime disconnectedAt, User disconnectedBy) {
		return new GuardianLinkHistory(link.getPatient(), link.getGuardian(), link.getConnectedAt(),
				disconnectedAt, disconnectedBy);
	}
}
