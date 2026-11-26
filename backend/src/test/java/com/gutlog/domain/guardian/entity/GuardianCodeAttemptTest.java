package com.gutlog.domain.guardian.entity;

import com.gutlog.domain.user.entity.Role;
import com.gutlog.domain.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 초대 코드 입력 실패 횟수와 차단 판단 단위 테스트 (G04, design.md 1장 C1).
 */
class GuardianCodeAttemptTest {

	private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 25, 12, 0);

	private GuardianCodeAttempt attempt;

	@BeforeEach
	void setUp() {
		User guardian = User.create("guardian1", "password", "보호자", Role.GUARDIAN, null);
		attempt = GuardianCodeAttempt.create(guardian);
	}

	@Test
	@DisplayName("처음에는 실패 0회이고 막혀 있지 않다")
	void initial() {
		assertThat(attempt.getFailCount()).isZero();
		assertThat(attempt.getBlockedUntil()).isNull();
		assertThat(attempt.isBlocked(NOW)).isFalse();
	}

	@Test
	@DisplayName("1·2번째 실패는 횟수만 올리고 막지 않는다")
	void firstAndSecondFailure() {
		attempt.recordFailure(NOW);
		assertThat(attempt.getFailCount()).isEqualTo(1);
		assertThat(attempt.isBlocked(NOW)).isFalse();

		attempt.recordFailure(NOW.plusSeconds(10));
		assertThat(attempt.getFailCount()).isEqualTo(2);
		assertThat(attempt.isBlocked(NOW.plusSeconds(10))).isFalse();
	}

	@Test
	@DisplayName("3번째 실패부터 그 시각 기준 1분 동안 막는다")
	void thirdFailureBlocks() {
		attempt.recordFailure(NOW);
		attempt.recordFailure(NOW.plusSeconds(10));
		LocalDateTime third = NOW.plusSeconds(20);
		attempt.recordFailure(third);

		assertThat(attempt.getBlockedUntil()).isEqualTo(third.plusMinutes(1));
		assertThat(attempt.isBlocked(third)).isTrue();
	}

	@Test
	@DisplayName("경계 - 1분이 되기 1초 전까지는 막히고, 1분이 되면 풀린다")
	void blockBoundary() {
		attempt.recordFailure(NOW);
		attempt.recordFailure(NOW);
		attempt.recordFailure(NOW);

		assertThat(attempt.isBlocked(NOW.plusMinutes(1).minusSeconds(1))).isTrue();
		assertThat(attempt.isBlocked(NOW.plusMinutes(1))).isFalse();
	}

	@Test
	@DisplayName("초기화하면 실패 0회, 차단 없음으로 돌아간다")
	void reset() {
		attempt.recordFailure(NOW);
		attempt.recordFailure(NOW);
		attempt.recordFailure(NOW);

		attempt.reset();

		assertThat(attempt.getFailCount()).isZero();
		assertThat(attempt.getBlockedUntil()).isNull();
		assertThat(attempt.isBlocked(NOW)).isFalse();
	}
}
