package com.gutlog.domain.record.medication.repository;

import com.gutlog.domain.record.medication.entity.MedicationLog;
import com.gutlog.domain.record.medication.entity.ScheduleType;
import com.gutlog.domain.record.medication.entity.UserMedicine;
import com.gutlog.domain.record.medication.entity.DosageForm;
import com.gutlog.domain.record.medication.entity.Medicine;
import com.gutlog.domain.record.medication.entity.MedicineCategory;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 복용 약·복용 기록 쿼리. deleted 조건, "그날 복용 중이던 약" 경계, 기간 경계, unique 제약을 실제 H2에서 확인한다.
 */
@DataJpaTest
class MedicationRepositoryTest {

    @Autowired UserRepository userRepository;
    @Autowired MedicineRepository medicineRepository;
    @Autowired UserMedicineRepository userMedicineRepository;
    @Autowired MedicationLogRepository medicationLogRepository;
    @Autowired EntityManager em;

    private static final LocalDate DAY = LocalDate.of(2026, 10, 6);

    private User me;
    private User other;
    private Medicine pentasa;
    private Medicine humira;

    @BeforeEach
    void setUp() {
        me = userRepository.save(User.create("me", "ENCODED", "나", Role.PATIENT, IbdType.CROHNS_DISEASE));
        other = userRepository.save(User.create("other", "ENCODED", "남", Role.GUARDIAN, null));
        pentasa = medicineRepository.save(Medicine.create("메살라진", "펜타사", DosageForm.TABLET, MedicineCategory.AMINOSALICYLATE));
        humira = medicineRepository.save(Medicine.create("아달리무맙", "휴미라", DosageForm.SC_INJECTION, MedicineCategory.BIOLOGIC));
    }

    private UserMedicine daily(User user, Medicine medicine) {
        return userMedicineRepository.save(UserMedicine.create(user, medicine, ScheduleType.DAILY, 3, null, null, null));
    }

    private MedicationLog log(UserMedicine um, LocalDate date, int dose) {
        return medicationLogRepository.save(MedicationLog.create(um, date, dose));
    }

    // 등록 시각(createdAt)은 auditing이 넣으므로, 경계 테스트를 위해 저장 후 직접 바꾼다.
    private void setCreatedAt(UserMedicine um, LocalDateTime createdAt) {
        em.flush();
        em.createQuery("update UserMedicine um set um.createdAt = :t where um.id = :id")
                .setParameter("t", createdAt).setParameter("id", um.getId()).executeUpdate();
        em.clear();
    }

    @Test
    @DisplayName("내 복용 약 목록 - 끊은 약과 남의 약은 빠지고, 약 정보가 함께 온다")
    void findActiveWithMedicine() {
        UserMedicine active = daily(me, pentasa);
        UserMedicine stopped = daily(me, humira);
        stopped.delete(LocalDateTime.now());
        daily(other, pentasa);
        em.flush();
        em.clear();

        List<UserMedicine> result = userMedicineRepository.findActiveWithMedicine(me.getId());

        assertThat(result).extracting(UserMedicine::getId).containsExactly(active.getId());
        assertThat(result.getFirst().getMedicine().getBrandName()).isEqualTo("펜타사");
    }

    @Test
    @DisplayName("활성 중복 확인 - 끊은 약은 중복으로 치지 않는다")
    void existsActive() {
        UserMedicine stopped = daily(me, pentasa);
        stopped.delete(LocalDateTime.now());
        em.flush();

        assertThat(userMedicineRepository.existsByUserIdAndMedicineIdAndDeletedFalse(me.getId(), pentasa.getId())).isFalse();

        daily(me, pentasa); // 끊었던 약은 새로 등록할 수 있다
        assertThat(userMedicineRepository.existsByUserIdAndMedicineIdAndDeletedFalse(me.getId(), pentasa.getId())).isTrue();
    }

    @Test
    @DisplayName("그날 복용 중이던 약 - 그날까지 등록했고, 안 끊었거나 그날 이후에 끊은 약")
    void findInUseOn() {
        LocalDateTime dayEnd = DAY.plusDays(1).atStartOfDay();
        UserMedicine registeredThatDay = daily(me, pentasa);
        UserMedicine registeredNextDay = daily(me, humira);
        UserMedicine stoppedNextDay = daily(me, pentasa);
        UserMedicine stoppedThatDay = daily(me, humira);
        stoppedNextDay.delete(dayEnd);                  // 다음날 0시에 끊음 → 그날은 복용 중
        stoppedThatDay.delete(dayEnd.minusMinutes(1));  // 그날 끊음 → 그날은 복용 중이 아님
        em.flush();
        setCreatedAt(registeredThatDay, dayEnd.minusSeconds(1));
        setCreatedAt(registeredNextDay, dayEnd);
        setCreatedAt(stoppedNextDay, DAY.minusDays(10).atStartOfDay());
        setCreatedAt(stoppedThatDay, DAY.minusDays(10).atStartOfDay());

        List<UserMedicine> result = userMedicineRepository.findInUseOn(me.getId(), dayEnd);

        assertThat(result).extracting(UserMedicine::getId)
                .containsExactly(registeredThatDay.getId(), stoppedNextDay.getId());
    }

    @Test
    @DisplayName("같은 약·날짜·회차는 한 행뿐 (unique). 삭제 표시된 행도 포함")
    void uniqueDose() {
        UserMedicine um = daily(me, pentasa);
        MedicationLog first = log(um, DAY, 1);
        first.delete();
        em.flush();

        assertThatThrownBy(() -> medicationLogRepository.saveAndFlush(MedicationLog.create(um, DAY, 1)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("단건 조회는 삭제 표시된 기록도 찾고, exists는 체크된 기록만 센다")
    void findIncludingDeleted() {
        UserMedicine um = daily(me, pentasa);
        log(um, DAY, 1).delete();
        em.flush();

        assertThat(medicationLogRepository.findByUserMedicineIdAndTakenDateAndDoseNumber(um.getId(), DAY, 1)).isPresent();
        assertThat(medicationLogRepository.existsByUserMedicineIdAndTakenDateAndDoseNumberAndDeletedFalse(um.getId(), DAY, 1)).isFalse();
    }

    @Test
    @DisplayName("그날 기록 - 끊은 약의 기록도 오고, 취소한 기록과 남의 기록은 빠진다")
    void findTakenOn() {
        UserMedicine um = daily(me, pentasa);
        UserMedicine stopped = daily(me, humira);
        MedicationLog dose2 = log(um, DAY, 2);
        MedicationLog dose1 = log(um, DAY, 1);
        MedicationLog stoppedDose = log(stopped, DAY, 1);
        log(um, DAY, 3).delete();
        log(um, DAY.plusDays(1), 1);
        log(daily(other, pentasa), DAY, 1);
        stopped.delete(LocalDateTime.now());
        em.flush();
        em.clear();

        List<MedicationLog> result = medicationLogRepository.findTakenOn(me.getId(), DAY);

        assertThat(result).extracting(MedicationLog::getId)
                .containsExactly(dose1.getId(), dose2.getId(), stoppedDose.getId());
        assertThat(result.get(2).getUserMedicine().getMedicine().getBrandName()).isEqualTo("휴미라");
    }

    @Test
    @DisplayName("기간 조회 - from 포함, end 제외. 날짜·약·회차 순")
    void findTakenBetween() {
        UserMedicine um = daily(me, pentasa);
        MedicationLog atFrom = log(um, DAY, 1);
        MedicationLog lastDay = log(um, DAY.plusDays(2), 1);
        log(um, DAY.minusDays(1), 1);   // 제외
        log(um, DAY.plusDays(3), 1);    // 제외 (end)

        List<MedicationLog> result = medicationLogRepository.findTakenBetween(me.getId(), DAY, DAY.plusDays(3));

        assertThat(result).extracting(MedicationLog::getId).containsExactly(atFrom.getId(), lastDay.getId());
    }

    @Test
    @DisplayName("마지막 투여일 - 조회 날짜까지(포함), 취소한 기록은 제외, 약별로 묶는다")
    void findLastTakenDates() {
        UserMedicine a = userMedicineRepository.save(UserMedicine.create(me, humira, ScheduleType.INTERVAL, null, 14, null, null));
        UserMedicine b = daily(me, pentasa);
        log(a, DAY.minusDays(14), 1);
        log(a, DAY, 1);
        log(a, DAY.plusDays(1), 1);          // 조회 날짜 이후 → 제외
        log(b, DAY.minusDays(3), 1);
        log(b, DAY.minusDays(1), 1).delete(); // 취소 → 제외
        em.flush();

        List<LastTakenDate> result = medicationLogRepository.findLastTakenDates(List.of(a.getId(), b.getId()), DAY);

        assertThat(result).containsExactlyInAnyOrder(
                new LastTakenDate(a.getId(), DAY),
                new LastTakenDate(b.getId(), DAY.minusDays(3)));
    }
}
