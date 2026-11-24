package com.gutlog.domain.guardian.repository;

import com.gutlog.domain.guardian.entity.GuardianCodeAttempt;
import com.gutlog.domain.user.entity.Role;
import com.gutlog.domain.user.entity.User;
import com.gutlog.domain.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 코드 입력 실패 기록 리포지토리. 보호자당 한 줄(UNIQUE)과 초기값을 확인한다.
 */
@DataJpaTest
class GuardianCodeAttemptRepositoryTest {

    @Autowired UserRepository userRepository;
    @Autowired GuardianCodeAttemptRepository attemptRepository;
    @Autowired EntityManager em;

    private User guardian;
    private User otherGuardian;

    @BeforeEach
    void setUp() {
        guardian = userRepository.save(User.create("guardian", "ENCODED", "보호자", Role.GUARDIAN, null));
        otherGuardian = userRepository.save(User.create("guardian2", "ENCODED", "보호자2", Role.GUARDIAN, null));
    }

    @Test
    @DisplayName("저장·조회 - 실패 0회, 차단 없음으로 시작한다")
    void saveWithDefaults() {
        GuardianCodeAttempt saved = attemptRepository.save(GuardianCodeAttempt.create(guardian));
        em.flush();
        em.clear();

        GuardianCodeAttempt found = attemptRepository.findByGuardianId(guardian.getId()).orElseThrow();
        assertThat(found.getId()).isEqualTo(saved.getId());
        assertThat(found.getFailCount()).isZero();
        assertThat(found.getBlockedUntil()).isNull();

        assertThat(attemptRepository.findByGuardianId(otherGuardian.getId())).isEmpty();
    }

    @Test
    @DisplayName("유니크 조건 - 보호자 1명에게 실패 기록 줄은 1개뿐이다")
    void uniquePerGuardian() {
        attemptRepository.save(GuardianCodeAttempt.create(guardian));
        attemptRepository.save(GuardianCodeAttempt.create(otherGuardian));   // 다른 보호자는 된다
        attemptRepository.flush();

        assertThatThrownBy(() -> attemptRepository.saveAndFlush(GuardianCodeAttempt.create(guardian)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
