package com.gutlog.domain.guardian.repository;

import com.gutlog.domain.guardian.entity.GuardianInviteCode;
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
 * 초대 코드 리포지토리. 환자당 한 줄, 코드 값 중복 불가(UNIQUE 2개)와 재발급 후 이전 코드 무효를 확인한다.
 */
@DataJpaTest
class GuardianInviteCodeRepositoryTest {

    @Autowired UserRepository userRepository;
    @Autowired GuardianInviteCodeRepository codeRepository;
    @Autowired EntityManager em;

    private static final LocalDateTime EXPIRES = LocalDateTime.of(2026, 11, 24, 12, 10);

    private User patientA;
    private User patientB;

    @BeforeEach
    void setUp() {
        patientA = userRepository.save(User.create("patientA", "ENCODED", "환자A", Role.PATIENT, IbdType.CROHNS_DISEASE));
        patientB = userRepository.save(User.create("patientB", "ENCODED", "환자B", Role.PATIENT, IbdType.ULCERATIVE_COLITIS));
    }

    private GuardianInviteCode save(User patient, String code) {
        return codeRepository.save(GuardianInviteCode.create(patient, code, EXPIRES));
    }

    @Test
    @DisplayName("저장·조회 - 환자 id와 코드 값으로 각각 찾는다")
    void saveAndFind() {
        GuardianInviteCode saved = save(patientA, "AB12CD");
        em.flush();
        em.clear();

        GuardianInviteCode byPatient = codeRepository.findByPatientId(patientA.getId()).orElseThrow();
        assertThat(byPatient.getId()).isEqualTo(saved.getId());
        assertThat(byPatient.getCode()).isEqualTo("AB12CD");
        assertThat(byPatient.getExpiresAt()).isEqualTo(EXPIRES);

        assertThat(codeRepository.findByCode("AB12CD")).get()
                .extracting(c -> c.getPatient().getId()).isEqualTo(patientA.getId());
        assertThat(codeRepository.findByCode("ZZ99ZZ")).isEmpty();
        assertThat(codeRepository.findByPatientId(patientB.getId())).isEmpty();
    }

    @Test
    @DisplayName("코드 존재 확인 - 새 코드를 만들 때 겹치는지 본다")
    void existsByCode() {
        save(patientA, "AB12CD");

        assertThat(codeRepository.existsByCode("AB12CD")).isTrue();
        assertThat(codeRepository.existsByCode("AB12CE")).isFalse();
    }

    @Test
    @DisplayName("재발급 - 같은 줄의 코드와 만료 시각이 바뀌고, 이전 코드로는 찾을 수 없다")
    void reissue() {
        GuardianInviteCode saved = save(patientA, "AB12CD");
        saved.reissue("XY34ZW", EXPIRES.plusMinutes(30));
        em.flush();
        em.clear();

        assertThat(codeRepository.findByCode("AB12CD")).isEmpty();
        GuardianInviteCode found = codeRepository.findByCode("XY34ZW").orElseThrow();
        assertThat(found.getId()).isEqualTo(saved.getId());
        assertThat(found.getExpiresAt()).isEqualTo(EXPIRES.plusMinutes(30));
        assertThat(codeRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("유니크 조건 - 환자 1명에게 코드 줄은 1개뿐이다")
    void uniquePerPatient() {
        save(patientA, "AB12CD");
        codeRepository.flush();

        assertThatThrownBy(() -> codeRepository.saveAndFlush(GuardianInviteCode.create(patientA, "XY34ZW", EXPIRES)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("유니크 조건 - 다른 환자도 같은 코드 값을 가질 수 없다")
    void uniqueCode() {
        save(patientA, "AB12CD");
        codeRepository.flush();

        assertThatThrownBy(() -> codeRepository.saveAndFlush(GuardianInviteCode.create(patientB, "AB12CD", EXPIRES)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("연결 성공 후 코드 삭제 - 지운 코드는 찾을 수 없고, 같은 환자가 새로 발급할 수 있다")
    void deleteThenCreateAgain() {
        GuardianInviteCode saved = save(patientA, "AB12CD");
        codeRepository.flush();
        codeRepository.delete(saved);
        codeRepository.flush();

        assertThat(codeRepository.findByCode("AB12CD")).isEmpty();
        codeRepository.saveAndFlush(GuardianInviteCode.create(patientA, "AB12CD", EXPIRES.plusDays(1)));
        assertThat(codeRepository.findByPatientId(patientA.getId())).isPresent();
    }

    @Test
    @DisplayName("잠금 조회 - 트랜잭션 안에서 코드 값으로 잠그고 찾는다 (G04, design.md 4.2의 5단계)")
    void findByCodeForUpdate() {
        save(patientA, "AB12CD");
        em.flush();
        em.clear();

        assertThat(codeRepository.findByCodeForUpdate("AB12CD")).get()
                .extracting(c -> c.getPatient().getId()).isEqualTo(patientA.getId());
        assertThat(codeRepository.findByCodeForUpdate("ZZ99ZZ")).isEmpty();
    }
}
