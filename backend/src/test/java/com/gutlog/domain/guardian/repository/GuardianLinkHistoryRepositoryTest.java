package com.gutlog.domain.guardian.repository;

import com.gutlog.domain.guardian.entity.GuardianLink;
import com.gutlog.domain.guardian.entity.GuardianLinkHistory;
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

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 해제 이력 리포지토리. 같은 쌍이 여러 번 해제되어도 이력은 줄마다 남는다. (유니크 없음)
 */
@DataJpaTest
class GuardianLinkHistoryRepositoryTest {

    @Autowired UserRepository userRepository;
    @Autowired GuardianLinkRepository linkRepository;
    @Autowired GuardianLinkHistoryRepository historyRepository;
    @Autowired EntityManager em;

    private static final LocalDateTime CONNECTED = LocalDateTime.of(2026, 11, 1, 9, 0);
    private static final LocalDateTime DISCONNECTED = LocalDateTime.of(2026, 11, 20, 18, 30);

    private User patient;
    private User guardian;

    @BeforeEach
    void setUp() {
        patient = userRepository.save(User.create("patient", "ENCODED", "환자", Role.PATIENT, IbdType.CROHNS_DISEASE));
        guardian = userRepository.save(User.create("guardian", "ENCODED", "보호자", Role.GUARDIAN, null));
    }

    @Test
    @DisplayName("저장·조회 - 연결에서 환자·보호자·연결 시각을 옮기고, 해제 시각과 해제한 사람을 남긴다")
    void saveFromLink() {
        GuardianLink link = linkRepository.save(GuardianLink.create(patient, guardian, CONNECTED));

        GuardianLinkHistory saved = historyRepository.save(GuardianLinkHistory.of(link, DISCONNECTED, guardian));
        em.flush();
        em.clear();

        GuardianLinkHistory found = historyRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getPatient().getId()).isEqualTo(patient.getId());
        assertThat(found.getGuardian().getId()).isEqualTo(guardian.getId());
        assertThat(found.getConnectedAt()).isEqualTo(CONNECTED);
        assertThat(found.getDisconnectedAt()).isEqualTo(DISCONNECTED);
        assertThat(found.getDisconnectedBy().getId()).isEqualTo(guardian.getId());
    }

    @Test
    @DisplayName("연결 행을 지워도 이력은 남는다")
    void historyKeptAfterLinkDeleted() {
        GuardianLink link = linkRepository.save(GuardianLink.create(patient, guardian, CONNECTED));
        GuardianLinkHistory saved = historyRepository.save(GuardianLinkHistory.of(link, DISCONNECTED, patient));
        linkRepository.delete(link);
        em.flush();
        em.clear();

        assertThat(linkRepository.findByPatientId(patient.getId())).isEmpty();
        assertThat(historyRepository.findById(saved.getId())).get()
                .extracting(h -> h.getDisconnectedBy().getId()).isEqualTo(patient.getId());
    }

    @Test
    @DisplayName("같은 환자·보호자가 여러 번 연결·해제해도 이력이 각각 남는다")
    void multipleHistoriesForSamePair() {
        GuardianLink first = linkRepository.save(GuardianLink.create(patient, guardian, CONNECTED));
        historyRepository.save(GuardianLinkHistory.of(first, DISCONNECTED, patient));
        linkRepository.delete(first);
        em.flush();

        GuardianLink second = linkRepository.save(GuardianLink.create(patient, guardian, DISCONNECTED.plusDays(1)));
        historyRepository.save(GuardianLinkHistory.of(second, DISCONNECTED.plusDays(2), guardian));
        em.flush();

        assertThat(historyRepository.count()).isEqualTo(2);
    }
}
