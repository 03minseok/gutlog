package com.gutlog.domain.guardian.entity;

import com.gutlog.domain.user.entity.User;
import com.gutlog.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 환자의 현재 초대 코드. 환자당 한 줄이다.
 * 재발급은 이 줄의 코드와 만료 시각을 바꾸므로 이전 코드는 바로 무효가 된다. 연결에 성공하면 이 줄을 지운다.
 * 코드는 원문으로 저장한다. (영문 대문자+숫자 6자리)
 */
@Entity
@Getter
@Table(name = "guardian_invite_code",
		uniqueConstraints = {
				@UniqueConstraint(name = "uk_guardian_invite_code_patient", columnNames = "patient_id"),
				@UniqueConstraint(name = "uk_guardian_invite_code_code", columnNames = "code")
		})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GuardianInviteCode extends BaseTimeEntity {
	public static final int CODE_LENGTH = 6;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "patient_id", nullable = false)
	private User patient;

	@Column(nullable = false, length = CODE_LENGTH)
	private String code;

	@Column(nullable = false)
	private LocalDateTime expiresAt;

	private GuardianInviteCode(User patient, String code, LocalDateTime expiresAt) {
		this.patient = patient;
		this.code = code;
		this.expiresAt = expiresAt;
	}

	public static GuardianInviteCode create(User patient, String code, LocalDateTime expiresAt) {
		return new GuardianInviteCode(patient, code, expiresAt);
	}

	public void reissue(String code, LocalDateTime expiresAt) {
		this.code = code;
		this.expiresAt = expiresAt;
	}

	/** 현재 시각이 만료 시각과 같거나 지나면 만료다. */
	public boolean isExpired(LocalDateTime now) {
		return !now.isBefore(expiresAt);
	}
}
