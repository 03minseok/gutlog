package com.gutlog.domain.record.medication.repository;

import com.gutlog.domain.record.medication.entity.DosageForm;
import com.gutlog.domain.record.medication.entity.MedicationSchedule;
import com.gutlog.domain.record.medication.entity.Medicine;
import com.gutlog.domain.record.medication.entity.MedicineCategory;
import com.gutlog.domain.record.medication.entity.ScheduleType;
import com.gutlog.domain.record.medication.entity.UserMedicine;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 복용 일정(기간별 이력) 쿼리. 일정은 [effectiveFrom, effectiveTo) 동안 적용된다. (시작일 포함, 종료일 제외)
 * 종료일이 null이면 지금 적용 중인 일정이다.
 */
@DataJpaTest
class MedicationScheduleRepositoryTest {

    @Autowired UserRepository userRepository;
    @Autowired MedicineRepository medicineRepository;
    @Autowired UserMedicineRepository userMedicineRepository;
    @Autowired MedicationScheduleRepository scheduleRepository;
    @Autowired EntityManager em;

    // 10/1 ~ 10/5: 하루 3회, 10/5 ~ : 하루 2회 (10/5에 수정)
    private static final LocalDate START = LocalDate.of(2026, 10, 1);
    private static final LocalDate CHANGED = LocalDate.of(2026, 10, 5);

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

    private UserMedicine userMedicine(User user, Medicine medicine) {
        return userMedicineRepository.save(UserMedicine.create(user, medicine));
    }

    private MedicationSchedule daily(UserMedicine um, LocalDate from, int count) {
        return scheduleRepository.save(MedicationSchedule.create(um, from, ScheduleType.DAILY, count, null, null, null));
    }

    /** 10/1에 하루 3회로 시작해서 10/5에 하루 2회로 바꾼 약 */
    private UserMedicine changedOnce(User user, Medicine medicine) {
        UserMedicine um = userMedicine(user, medicine);
        daily(um, START, 3).close(CHANGED);
        daily(um, CHANGED, 2);
        em.flush();
        em.clear();
        return um;
    }

    @Test
    @DisplayName("현재 일정 - 닫히지 않은 일정 하나만 찾는다")
    void findCurrent() {
        UserMedicine um = changedOnce(me, pentasa);

        MedicationSchedule current = scheduleRepository.findCurrent(um.getId()).orElseThrow();

        assertThat(current.getDailyDoseCount()).isEqualTo(2);
        assertThat(current.getEffectiveTo()).isNull();
    }

    @Test
    @DisplayName("그날 일정 - 시작일은 새 일정, 종료일은 그 일정에 포함되지 않는다")
    void findInEffectOnBoundary() {
        UserMedicine um = changedOnce(me, pentasa);

        assertThat(scheduleRepository.findInEffectOn(um.getId(), START)).get()
                .extracting(MedicationSchedule::getDailyDoseCount).isEqualTo(3);
        assertThat(scheduleRepository.findInEffectOn(um.getId(), CHANGED.minusDays(1))).get()
                .extracting(MedicationSchedule::getDailyDoseCount).isEqualTo(3);
        assertThat(scheduleRepository.findInEffectOn(um.getId(), CHANGED)).get()
                .extracting(MedicationSchedule::getDailyDoseCount).isEqualTo(2);
        assertThat(scheduleRepository.findInEffectOn(um.getId(), CHANGED.plusDays(30))).get()
                .extracting(MedicationSchedule::getDailyDoseCount).isEqualTo(2);
    }

    @Test
    @DisplayName("그날 일정 - 등록 전 날짜에는 일정이 없다")
    void findInEffectOnBeforeStart() {
        UserMedicine um = changedOnce(me, pentasa);

        assertThat(scheduleRepository.findInEffectOn(um.getId(), START.minusDays(1))).isEmpty();
    }

    @Test
    @DisplayName("그날 일정 - 끊은 날(일정 종료일)에는 일정이 없다")
    void findInEffectOnStoppedDay() {
        UserMedicine um = userMedicine(me, pentasa);
        daily(um, START, 1).close(CHANGED);
        um.delete(CHANGED.atTime(9, 0));
        em.flush();

        assertThat(scheduleRepository.findInEffectOn(um.getId(), CHANGED.minusDays(1))).isPresent();
        assertThat(scheduleRepository.findInEffectOn(um.getId(), CHANGED)).isEmpty();
    }

    @Test
    @DisplayName("사용자의 그날 일정 - 날짜에 맞는 일정이 오고, 남의 약은 빠지며, 약 정보까지 한 번에 온다")
    void findAllInEffectOn() {
        UserMedicine changed = changedOnce(me, pentasa);
        UserMedicine later = userMedicine(me, humira);
        daily(later, CHANGED.plusDays(1), 1);   // 10/6부터 → 10/4에는 없음
        changedOnce(other, pentasa);
        em.flush();
        em.clear();

        List<MedicationSchedule> before = scheduleRepository.findAllInEffectOn(me.getId(), CHANGED.minusDays(1));
        List<MedicationSchedule> after = scheduleRepository.findAllInEffectOn(me.getId(), CHANGED.plusDays(1));

        assertThat(before).extracting(s -> s.getUserMedicine().getId()).containsExactly(changed.getId());
        assertThat(before.getFirst().getDailyDoseCount()).isEqualTo(3);
        assertThat(before.getFirst().getUserMedicine().getMedicine().getBrandName()).isEqualTo("펜타사");
        assertThat(after).extracting(s -> s.getUserMedicine().getId()).containsExactlyInAnyOrder(changed.getId(), later.getId());
    }

    @Test
    @DisplayName("사용자의 그날 일정 - 지금은 끊은 약도 그때 일정이 있었으면 그 날짜에는 나온다")
    void findAllInEffectOnStoppedLater() {
        UserMedicine um = userMedicine(me, pentasa);
        daily(um, START, 2).close(CHANGED);
        um.delete(CHANGED.atTime(9, 0));
        em.flush();

        assertThat(scheduleRepository.findAllInEffectOn(me.getId(), START)).hasSize(1);
        assertThat(scheduleRepository.findAllInEffectOn(me.getId(), CHANGED)).isEmpty();
    }

    @Test
    @DisplayName("내 복용 약 목록 - 끊지 않은 약의 현재 일정만, 약 id 순으로")
    void findCurrentByUserId() {
        UserMedicine changed = changedOnce(me, pentasa);
        UserMedicine stopped = userMedicine(me, humira);
        daily(stopped, START, 1).close(CHANGED);
        stopped.delete(LocalDateTime.now());
        em.flush();
        em.clear();

        List<MedicationSchedule> result = scheduleRepository.findCurrentByUserId(me.getId());

        assertThat(result).extracting(s -> s.getUserMedicine().getId()).containsExactly(changed.getId());
        assertThat(result.getFirst().getDailyDoseCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("그 날짜까지 시작한 마지막 일정 - 그날 끊은 약(일정이 그날 닫힘)을 보여줄 때 쓴다")
    void findLatestStartedBy() {
        UserMedicine um = changedOnce(me, pentasa);   // 10/1 ~ 10/5: 3회, 10/5 ~ : 2회

        assertThat(scheduleRepository.findLatestStartedBy(List.of(um.getId()), CHANGED.minusDays(1)))
                .singleElement().extracting(MedicationSchedule::getDailyDoseCount).isEqualTo(3);
        assertThat(scheduleRepository.findLatestStartedBy(List.of(um.getId()), CHANGED.plusDays(3)))
                .singleElement().extracting(MedicationSchedule::getDailyDoseCount).isEqualTo(2);
        assertThat(scheduleRepository.findLatestStartedBy(List.of(um.getId()), START.minusDays(1))).isEmpty();
    }
}
