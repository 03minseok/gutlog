package com.gutlog.domain.guardian.entity;

import com.gutlog.domain.user.entity.User;
import com.gutlog.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 보호자별 초대 코드 입력 실패 횟수와 차단 시각. 보호자당 한 줄이다.
 * 서버를 재시작해도 차단이 유지되도록 DB에 둔다.
 * 실패 횟수를 올리고 차단·초기화하는 동작은 G04에서 코드 입력 규칙과 함께 만든다.
 */
@Entity
@Getter
@Table(name = "guardian_code_attempt",
		uniqueConstraints = @UniqueConstraint(name = "uk_guardian_code_attempt_guardian", columnNames = "guardian_id"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GuardianCodeAttempt extends BaseTimeEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "guardian_id", nullable = false)
	private User guardian;

	@Column(nullable = false)
	private int failCount;

	/** 이 시각 전까지 입력을 막는다. 막힌 적이 없으면 null이다. */
	private LocalDateTime blockedUntil;

	private GuardianCodeAttempt(User guardian) {
		this.guardian = guardian;
	}

	/** 실패 0회, 차단 없음으로 시작한다. */
	public static GuardianCodeAttempt create(User guardian) {
		return new GuardianCodeAttempt(guardian);
	}
}
