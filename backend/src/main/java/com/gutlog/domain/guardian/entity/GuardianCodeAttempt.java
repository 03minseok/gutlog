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
 * 3번째 오답부터 1분 동안 막는다. 1분이 지나거나 연결에 성공하면 횟수는 0이 된다. (design.md 1장 C1)
 */
@Entity
@Getter
@Table(name = "guardian_code_attempt",
		uniqueConstraints = @UniqueConstraint(name = "uk_guardian_code_attempt_guardian", columnNames = "guardian_id"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GuardianCodeAttempt extends BaseTimeEntity {
	public static final int MAX_FAIL_COUNT = 3;
	public static final int BLOCK_MINUTES = 1;

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

	// 지금 입력이 막혀 있는지 판단한다 (design.md 1장 C1, 4.2의 3단계)
	public boolean isBlocked(LocalDateTime now) {
		return blockedUntil!=null && now.isBefore(blockedUntil);
	}

	// 코드가 틀렸을 때 실패 횟수를 올리고, 3번째면 1분 동안 막는다 (design.md 1장 C1, 4.2의 5단계)
	public void recordFailure(LocalDateTime now) {
		if(blockedUntil!=null && (now.isEqual(blockedUntil)|| now.isAfter(blockedUntil) )){
			reset();
		}
		failCount++;
		if(failCount==MAX_FAIL_COUNT){
			blockedUntil = now.plusMinutes(BLOCK_MINUTES);
		}
	}

	// 실패 횟수와 차단을 처음 상태로 되돌린다 (design.md 1장 C1, 4.2의 7단계)
	public void reset() {
		failCount = 0;
		blockedUntil = null;
	}
}
