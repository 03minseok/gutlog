package com.gutlog.domain.guardian.repository;

import com.gutlog.domain.guardian.entity.GuardianAccess;
import com.gutlog.domain.guardian.entity.GuardianLink;
import com.gutlog.domain.user.entity.IbdType;
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

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 현재 연결 리포지토리. 환자당 연결 1개(UNIQUE)와 공개 범위 기본값을 실제 H2에서 확인한다.
 */
@DataJpaTest
class GuardianLinkRepositoryTest {

    @Autowired UserRepository userRepository;
    @Autowired GuardianLinkRepository linkRepository;
    @Autowired EntityManager em;

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 24, 12, 0);

    private User patientA;
    private User patientB;
    private User guardian;
    private User otherGuardian;

    @BeforeEach
    void setUp() {
        patientA = userRepository.save(User.create("patientA", "ENCODED", "환자A", Role.PATIENT, IbdType.CROHNS_DISEASE));
        patientB = userRepository.save(User.create("patientB", "ENCODED", "환자B", Role.PATIENT, IbdType.ULCERATIVE_COLITIS));
        guardian = userRepository.save(User.create("guardian", "ENCODED", "보호자", Role.GUARDIAN, null));
        otherGuardian = userRepository.save(User.create("guardian2", "ENCODED", "보호자2", Role.GUARDIAN, null));
    }

    private GuardianLink link(User patient, User guardian, LocalDateTime connectedAt) {
        return linkRepository.save(GuardianLink.create(patient, guardian, connectedAt));
    }

    @Test
    @DisplayName("저장·조회 - 공개 범위 5개는 전부 비공개로 시작한다")
    void saveWithPrivateDefaults() {
        GuardianLink saved = link(patientA, guardian, NOW);
        em.flush();
        em.clear();

        GuardianLink found = linkRepository.findByPatientId(patientA.getId()).orElseThrow();

        assertThat(found.getId()).isEqualTo(saved.getId());
        assertThat(found.getGuardian().getId()).isEqualTo(guardian.getId());
        assertThat(found.getConnectedAt()).isEqualTo(NOW);
        assertThat(found.getMealAccess()).isEqualTo(GuardianAccess.PRIVATE);
        assertThat(found.getBowelAccess()).isEqualTo(GuardianAccess.PRIVATE);
        assertThat(found.getMedicationAccess()).isEqualTo(GuardianAccess.PRIVATE);
        assertThat(found.getWeightAccess()).isEqualTo(GuardianAccess.PRIVATE);
        assertThat(found.getSleepAccess()).isEqualTo(GuardianAccess.PRIVATE);
    }

    @Test
    @DisplayName("공개 범위 변경 - 바꾼 값이 종류별로 저장된다")
    void updateAccess() {
        GuardianLink saved = link(patientA, guardian, NOW);
        saved.updateAccess(GuardianAccess.EDIT, GuardianAccess.VIEW, GuardianAccess.PRIVATE,
                GuardianAccess.VIEW, GuardianAccess.EDIT);
        em.flush();
        em.clear();

        GuardianLink found = linkRepository.findById(saved.getId()).orElseThrow();

        assertThat(found.getMealAccess()).isEqualTo(GuardianAccess.EDIT);
        assertThat(found.getBowelAccess()).isEqualTo(GuardianAccess.VIEW);
        assertThat(found.getMedicationAccess()).isEqualTo(GuardianAccess.PRIVATE);
        assertThat(found.getWeightAccess()).isEqualTo(GuardianAccess.VIEW);
        assertThat(found.getSleepAccess()).isEqualTo(GuardianAccess.EDIT);
    }

    @Test
    @DisplayName("환자와 보호자로 조회 - 연결된 쌍만 찾는다")
    void findByPatientAndGuardian() {
        link(patientA, guardian, NOW);

        assertThat(linkRepository.findByPatientIdAndGuardianId(patientA.getId(), guardian.getId())).isPresent();
        assertThat(linkRepository.findByPatientIdAndGuardianId(patientA.getId(), otherGuardian.getId())).isEmpty();
        assertThat(linkRepository.findByPatientIdAndGuardianId(patientB.getId(), guardian.getId())).isEmpty();
    }

    @Test
    @DisplayName("보호자의 연결 목록 - 보호자 1명은 환자 2명과 연결될 수 있고, 먼저 연결한 순으로 온다")
    void findAllByGuardian() {
        GuardianLink later = link(patientB, guardian, NOW.plusMinutes(5));
        GuardianLink earlier = link(patientA, guardian, NOW);
        em.flush();

        assertThat(linkRepository.findAllByGuardianIdOrderByConnectedAtAscIdAsc(guardian.getId()))
                .extracting(GuardianLink::getId)
                .containsExactly(earlier.getId(), later.getId());
        assertThat(linkRepository.countByGuardianId(guardian.getId())).isEqualTo(2);
        assertThat(linkRepository.countByGuardianId(otherGuardian.getId())).isZero();
    }

    @Test
    @DisplayName("유니크 조건 - 환자 1명에게 보호자 연결은 1개뿐이다")
    void uniquePerPatient() {
        link(patientA, guardian, NOW);
        linkRepository.flush();

        assertThatThrownBy(() -> linkRepository.saveAndFlush(GuardianLink.create(patientA, otherGuardian, NOW)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("해제 후 재연결 - 행을 지우면 같은 환자를 다시 연결할 수 있고 공개 범위는 비공개로 시작한다")
    void reconnectAfterDelete() {
        GuardianLink first = link(patientA, guardian, NOW);
        first.updateAccess(GuardianAccess.EDIT, GuardianAccess.EDIT, GuardianAccess.EDIT,
                GuardianAccess.EDIT, GuardianAccess.EDIT);
        linkRepository.flush();
        linkRepository.delete(first);
        linkRepository.flush();

        GuardianLink again = linkRepository.saveAndFlush(GuardianLink.create(patientA, guardian, NOW.plusDays(1)));
        em.clear();

        GuardianLink found = linkRepository.findByPatientId(patientA.getId()).orElseThrow();
        assertThat(found.getId()).isEqualTo(again.getId());
        assertThat(found.getMealAccess()).isEqualTo(GuardianAccess.PRIVATE);
    }
}
