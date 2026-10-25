package com.gutlog.domain.record.medication.service;

import com.gutlog.domain.record.medication.dto.response.DailyMedicationResponse;
import com.gutlog.domain.record.medication.dto.response.MedicationToggleResponse;
import com.gutlog.domain.record.medication.entity.DosageForm;
import com.gutlog.domain.record.medication.entity.MedicationLog;
import com.gutlog.domain.record.medication.entity.MedicationSchedule;
import com.gutlog.domain.record.medication.entity.Medicine;
import com.gutlog.domain.record.medication.entity.MedicineCategory;
import com.gutlog.domain.record.medication.entity.ScheduleType;
import com.gutlog.domain.record.medication.entity.UserMedicine;
import com.gutlog.domain.record.medication.repository.LastTakenDate;
import com.gutlog.domain.record.medication.repository.MedicationLogRepository;
import com.gutlog.domain.record.medication.repository.MedicationScheduleRepository;
import com.gutlog.domain.record.medication.repository.UserMedicineRepository;
import com.gutlog.domain.user.entity.IbdType;
import com.gutlog.domain.user.entity.Role;
import com.gutlog.domain.user.entity.User;
import com.gutlog.global.exception.BusinessException;
import com.gutlog.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.*;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * MedicationLogService 단위 테스트. 오늘 = 2026-10-06 (서울)로 고정한다.
 * 회차 상한·복용 방식은 "그 날짜에 적용된 일정(MedicationSchedule)"이 정한다.
 */
@ExtendWith(MockitoExtension.class)
class MedicationLogServiceTest {

    @Mock MedicationLogRepository medicationLogRepository;
    @Mock MedicationScheduleRepository scheduleRepository;
    @Mock UserMedicineRepository userMedicineRepository;

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 6);
    // 서울 2026-10-06 00:30 = UTC 2026-10-05 15:30. UTC 날짜로 "오늘"을 구하면 10-06이 미래가 되어 테스트가 깨진다.
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-05T15:30:00Z"), ZoneId.of("Asia/Seoul"));
    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;
    private static final Long UM_ID = 5L;

    private MedicationLogService service;

    @BeforeEach
    void setUp() {
        service = new MedicationLogService(medicationLogRepository, scheduleRepository, userMedicineRepository, CLOCK);
    }

    private static User user(Long id) {
        User user = User.create("user" + id, "ENCODED", "닉" + id, Role.PATIENT, IbdType.ULCERATIVE_COLITIS);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private static UserMedicine um(Long id, Long ownerId) {
        UserMedicine um = UserMedicine.create(user(ownerId), Medicine.create("성분", "약" + id, DosageForm.TABLET, MedicineCategory.OTHER));
        ReflectionTestUtils.setField(um, "id", id);
        return um;
    }

    private static MedicationSchedule schedule(UserMedicine um, ScheduleType type, Integer dailyDoseCount, Integer intervalDays) {
        return MedicationSchedule.create(um, TODAY.minusDays(30), type, dailyDoseCount, intervalDays, null, null);
    }

    private static MedicationSchedule daily(UserMedicine um, int count) {
        return schedule(um, ScheduleType.DAILY, count, null);
    }

    private static MedicationLog log(UserMedicine um, LocalDate date, int dose) {
        return MedicationLog.create(um, date, dose);
    }

    private static ErrorCode errorCodeOf(Throwable e) {
        return ((BusinessException) e).getErrorCode();
    }

    /** 내 약(UM_ID) + 오늘 적용 중인 하루 count회 일정 */
    private UserMedicine givenDailyMedicine(int count) {
        UserMedicine um = um(UM_ID, USER_ID);
        given(userMedicineRepository.findById(UM_ID)).willReturn(Optional.of(um));
        given(scheduleRepository.findInEffectOn(UM_ID, TODAY)).willReturn(Optional.of(daily(um, count)));
        return um;
    }

    private void givenLog(int dose, MedicationLog log) {
        given(medicationLogRepository.findByUserMedicineIdAndTakenDateAndDoseNumber(UM_ID, TODAY, dose)).willReturn(Optional.ofNullable(log));
    }

    private void givenTaken(int dose, boolean taken) {
        given(medicationLogRepository.existsByUserMedicineIdAndTakenDateAndDoseNumberAndDeletedFalse(UM_ID, TODAY, dose)).willReturn(taken);
    }

    @Nested
    @DisplayName("토글 - 체크")
    class Check {

        @Test
        @DisplayName("기록이 없으면 새로 만든다 (1회차는 앞 회차 확인 없음)")
        void createNew() {
            givenDailyMedicine(3);
            givenLog(1, null);

            MedicationToggleResponse response = service.toggleMedicationLog(USER_ID, UM_ID, TODAY, 1);

            ArgumentCaptor<MedicationLog> captor = ArgumentCaptor.forClass(MedicationLog.class);
            verify(medicationLogRepository).save(captor.capture());
            assertThat(captor.getValue().getDoseNumber()).isEqualTo(1);
            assertThat(captor.getValue().getTakenDate()).isEqualTo(TODAY);
            assertThat(response.taken()).isTrue();
        }

        @Test
        @DisplayName("취소했던 회차를 다시 체크하면 새로 넣지 않고 삭제 표시를 되돌린다")
        void restoreDeleted() {
            UserMedicine um = givenDailyMedicine(3);
            MedicationLog cancelled = log(um, TODAY, 1);
            cancelled.delete();
            givenLog(1, cancelled);

            MedicationToggleResponse response = service.toggleMedicationLog(USER_ID, UM_ID, TODAY, 1);

            assertThat(cancelled.isDeleted()).isFalse();
            assertThat(response.taken()).isTrue();
            verify(medicationLogRepository, never()).save(any());
        }

        @Test
        @DisplayName("2회차는 1회차가 체크되어 있어야 한다")
        void orderRequired() {
            givenDailyMedicine(3);
            givenLog(2, null);
            givenTaken(1, false);

            assertThatThrownBy(() -> service.toggleMedicationLog(USER_ID, UM_ID, TODAY, 2))
                    .extracting(MedicationLogServiceTest::errorCodeOf).isEqualTo(ErrorCode.DOSE_ORDER_CHECK);
            verify(medicationLogRepository, never()).save(any());
        }

        @Test
        @DisplayName("1회차가 체크되어 있으면 2회차를 체크할 수 있다")
        void orderSatisfied() {
            givenDailyMedicine(3);
            givenLog(2, null);
            givenTaken(1, true);

            assertThat(service.toggleMedicationLog(USER_ID, UM_ID, TODAY, 2).taken()).isTrue();
        }

        @Test
        @DisplayName("회차가 그날 일정의 하루 복용 횟수보다 크면 INVALID_DOSE_NUMBER")
        void overDailyCount() {
            givenDailyMedicine(2);
            givenLog(3, null);

            assertThatThrownBy(() -> service.toggleMedicationLog(USER_ID, UM_ID, TODAY, 3))
                    .extracting(MedicationLogServiceTest::errorCodeOf).isEqualTo(ErrorCode.INVALID_DOSE_NUMBER);
        }

        @Test
        @DisplayName("회차 상한은 체크하는 날짜의 일정으로 정한다 (지난 날짜는 그때 하루 3회, 오늘은 1회)")
        void doseLimitByScheduleOfThatDate() {
            UserMedicine um = um(UM_ID, USER_ID);
            LocalDate past = TODAY.minusDays(3);
            given(userMedicineRepository.findById(UM_ID)).willReturn(Optional.of(um));
            given(scheduleRepository.findInEffectOn(UM_ID, past)).willReturn(Optional.of(daily(um, 3)));
            given(medicationLogRepository.findByUserMedicineIdAndTakenDateAndDoseNumber(UM_ID, past, 3)).willReturn(Optional.empty());
            given(medicationLogRepository.existsByUserMedicineIdAndTakenDateAndDoseNumberAndDeletedFalse(UM_ID, past, 2)).willReturn(true);

            assertThat(service.toggleMedicationLog(USER_ID, UM_ID, past, 3).taken()).isTrue();
        }

        @Test
        @DisplayName("그날 일정이 없으면(등록 전 날짜) NOT_IN_USE_ON_DATE, 저장하지 않는다")
        void noScheduleOnDate() {
            UserMedicine um = um(UM_ID, USER_ID);
            LocalDate beforeRegistration = TODAY.minusDays(40);
            given(userMedicineRepository.findById(UM_ID)).willReturn(Optional.of(um));
            given(medicationLogRepository.findByUserMedicineIdAndTakenDateAndDoseNumber(UM_ID, beforeRegistration, 1)).willReturn(Optional.empty());
            given(scheduleRepository.findInEffectOn(UM_ID, beforeRegistration)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.toggleMedicationLog(USER_ID, UM_ID, beforeRegistration, 1))
                    .extracting(MedicationLogServiceTest::errorCodeOf).isEqualTo(ErrorCode.NOT_IN_USE_ON_DATE);
            verify(medicationLogRepository, never()).save(any());
        }

        @Test
        @DisplayName("회차가 0 이하면 INVALID_DOSE_NUMBER")
        void zeroDose() {
            given(userMedicineRepository.findById(UM_ID)).willReturn(Optional.of(um(UM_ID, USER_ID)));

            assertThatThrownBy(() -> service.toggleMedicationLog(USER_ID, UM_ID, TODAY, 0))
                    .extracting(MedicationLogServiceTest::errorCodeOf).isEqualTo(ErrorCode.INVALID_DOSE_NUMBER);
        }

        @Test
        @DisplayName("INTERVAL·AS_NEEDED는 회차 1만 된다")
        void nonDailyOnlyDoseOne() {
            UserMedicine um = um(UM_ID, USER_ID);
            given(userMedicineRepository.findById(UM_ID)).willReturn(Optional.of(um));
            given(scheduleRepository.findInEffectOn(UM_ID, TODAY)).willReturn(Optional.of(schedule(um, ScheduleType.INTERVAL, null, 14)));
            givenLog(2, null);

            assertThatThrownBy(() -> service.toggleMedicationLog(USER_ID, UM_ID, TODAY, 2))
                    .extracting(MedicationLogServiceTest::errorCodeOf).isEqualTo(ErrorCode.INVALID_DOSE_NUMBER);
        }
    }

    @Nested
    @DisplayName("토글 - 취소")
    class Uncheck {

        @Test
        @DisplayName("체크된 회차는 소프트 딜리트한다")
        void softDelete() {
            UserMedicine um = um(UM_ID, USER_ID);
            MedicationLog taken = log(um, TODAY, 1);
            given(userMedicineRepository.findById(UM_ID)).willReturn(Optional.of(um));
            givenLog(1, taken);
            givenTaken(2, false);

            MedicationToggleResponse response = service.toggleMedicationLog(USER_ID, UM_ID, TODAY, 1);

            assertThat(taken.isDeleted()).isTrue();
            assertThat(response.taken()).isFalse();
            verify(medicationLogRepository, never()).delete(any());
        }

        @Test
        @DisplayName("2회차가 체크되어 있으면 1회차를 취소할 수 없다")
        void orderRequired() {
            UserMedicine um = um(UM_ID, USER_ID);
            MedicationLog taken = log(um, TODAY, 1);
            given(userMedicineRepository.findById(UM_ID)).willReturn(Optional.of(um));
            givenLog(1, taken);
            givenTaken(2, true);

            assertThatThrownBy(() -> service.toggleMedicationLog(USER_ID, UM_ID, TODAY, 1))
                    .extracting(MedicationLogServiceTest::errorCodeOf).isEqualTo(ErrorCode.DOSE_ORDER_UNCHECK);
            assertThat(taken.isDeleted()).isFalse();
        }

        @Test
        @DisplayName("취소는 일정을 보지 않는다 - 같은 날 횟수를 줄여 범위 밖이 된 회차도 취소할 수 있다")
        void outOfRangeCanBeUnchecked() {
            UserMedicine um = um(UM_ID, USER_ID);
            MedicationLog dose3 = log(um, TODAY, 3);
            given(userMedicineRepository.findById(UM_ID)).willReturn(Optional.of(um));
            givenLog(3, dose3);
            givenTaken(4, false);

            assertThat(service.toggleMedicationLog(USER_ID, UM_ID, TODAY, 3).taken()).isFalse();
            assertThat(dose3.isDeleted()).isTrue();
            verify(scheduleRepository, never()).findInEffectOn(any(), any());
        }
    }

    @Nested
    @DisplayName("토글 - 공통 거절")
    class Rejected {

        @Test
        @DisplayName("남의 약이면 404")
        void notMine() {
            given(userMedicineRepository.findById(UM_ID)).willReturn(Optional.of(um(UM_ID, OTHER_USER_ID)));

            assertThatThrownBy(() -> service.toggleMedicationLog(USER_ID, UM_ID, TODAY, 1))
                    .extracting(MedicationLogServiceTest::errorCodeOf).isEqualTo(ErrorCode.USER_MEDICINE_NOT_FOUND);
        }

        @Test
        @DisplayName("끊은 약은 체크도 취소도 할 수 없다")
        void stopped() {
            UserMedicine um = um(UM_ID, USER_ID);
            um.delete(LocalDateTime.now(CLOCK));
            given(userMedicineRepository.findById(UM_ID)).willReturn(Optional.of(um));

            assertThatThrownBy(() -> service.toggleMedicationLog(USER_ID, UM_ID, TODAY, 1))
                    .extracting(MedicationLogServiceTest::errorCodeOf).isEqualTo(ErrorCode.STOPPED_MEDICINE);
            verify(medicationLogRepository, never()).findByUserMedicineIdAndTakenDateAndDoseNumber(any(), any(), anyInt());
        }

        @Test
        @DisplayName("미래 날짜는 거절, 오늘(서울 기준)은 허용")
        void futureDate() {
            givenDailyMedicine(1);
            givenLog(1, null);

            assertThatThrownBy(() -> service.toggleMedicationLog(USER_ID, UM_ID, TODAY.plusDays(1), 1))
                    .extracting(MedicationLogServiceTest::errorCodeOf).isEqualTo(ErrorCode.FUTURE_DATE_NOT_ALLOWED);
            assertThat(service.toggleMedicationLog(USER_ID, UM_ID, TODAY, 1).taken()).isTrue();
        }
    }

    @Nested
    @DisplayName("날짜별 조회")
    class Daily {

        @Test
        @DisplayName("n/m은 그날 일정이 DAILY인 약의 범위 안 회차만 센다")
        void counts() {
            UserMedicine a = um(1L, USER_ID);
            UserMedicine b = um(2L, USER_ID);   // 3회차 기록이 남아 있지만 그날 일정은 2회
            UserMedicine interval = um(3L, USER_ID);
            UserMedicine asNeeded = um(4L, USER_ID);
            given(scheduleRepository.findAllInEffectOn(USER_ID, TODAY)).willReturn(List.of(
                    daily(a, 3), daily(b, 2),
                    schedule(interval, ScheduleType.INTERVAL, null, 14),
                    schedule(asNeeded, ScheduleType.AS_NEEDED, null, null)));
            given(medicationLogRepository.findTakenOn(USER_ID, TODAY)).willReturn(List.of(
                    log(a, TODAY, 1), log(a, TODAY, 2),
                    log(b, TODAY, 1), log(b, TODAY, 2), log(b, TODAY, 3),
                    log(interval, TODAY, 1), log(asNeeded, TODAY, 1)));
            given(medicationLogRepository.findLastTakenDates(List.of(3L), TODAY))
                    .willReturn(List.of(new LastTakenDate(3L, TODAY)));

            DailyMedicationResponse response = service.getDailyMedication(USER_ID, TODAY);

            assertThat(response.dailyTotalCount()).isEqualTo(5);   // 3 + 2
            assertThat(response.dailyTakenCount()).isEqualTo(4);   // a 2개 + b 범위 안 2개
            assertThat(response.medicines()).extracting(DailyMedicationResponse.Item::userMedicineId).containsExactly(1L, 2L, 3L, 4L);
            assertThat(response.medicines().get(1).takenDoses()).containsExactly(1, 2, 3);
            verify(scheduleRepository, never()).findLatestStartedBy(anyList(), any());
        }

        @Test
        @DisplayName("응답의 복용 방식·횟수는 그 날짜에 적용된 일정 값이다")
        void itemUsesScheduleOfThatDate() {
            UserMedicine um = um(1L, USER_ID);
            LocalDate past = TODAY.minusDays(10);
            given(scheduleRepository.findAllInEffectOn(USER_ID, past)).willReturn(List.of(daily(um, 3)));
            given(medicationLogRepository.findTakenOn(USER_ID, past)).willReturn(List.of());

            DailyMedicationResponse response = service.getDailyMedication(USER_ID, past);

            assertThat(response.medicines().getFirst().scheduleType()).isEqualTo(ScheduleType.DAILY);
            assertThat(response.medicines().getFirst().dailyDoseCount()).isEqualTo(3);
            assertThat(response.dailyTotalCount()).isEqualTo(3);
        }

        @Test
        @DisplayName("INTERVAL 약은 마지막 투여일과 다음 예정일을 준다. 기록이 없으면 null")
        void intervalDates() {
            UserMedicine humira = um(3L, USER_ID);
            UserMedicine neverTaken = um(4L, USER_ID);
            given(scheduleRepository.findAllInEffectOn(USER_ID, TODAY)).willReturn(List.of(
                    schedule(humira, ScheduleType.INTERVAL, null, 14),
                    schedule(neverTaken, ScheduleType.INTERVAL, null, 56)));
            given(medicationLogRepository.findTakenOn(USER_ID, TODAY)).willReturn(List.of());
            given(medicationLogRepository.findLastTakenDates(anyCollection(), any()))
                    .willReturn(List.of(new LastTakenDate(3L, TODAY.minusDays(10))));

            DailyMedicationResponse response = service.getDailyMedication(USER_ID, TODAY);

            assertThat(response.medicines().get(0).lastTakenDate()).isEqualTo(TODAY.minusDays(10));
            assertThat(response.medicines().get(0).nextDueDate()).isEqualTo(TODAY.plusDays(4));
            assertThat(response.medicines().get(1).lastTakenDate()).isNull();
            assertThat(response.medicines().get(1).nextDueDate()).isNull();
            assertThat(response.dailyTotalCount()).isZero();
        }

        @Test
        @DisplayName("그날 일정이 없는(그날 끊은) 약도 기록이 있으면 그 전 마지막 일정으로 보여주되, n/m에는 세지 않는다")
        void stoppedWithLogs() {
            UserMedicine stopped = um(9L, USER_ID);
            stopped.delete(LocalDateTime.now(CLOCK));
            given(scheduleRepository.findAllInEffectOn(USER_ID, TODAY)).willReturn(List.of());
            given(medicationLogRepository.findTakenOn(USER_ID, TODAY)).willReturn(List.of(log(stopped, TODAY, 1)));
            given(scheduleRepository.findLatestStartedBy(List.of(9L), TODAY)).willReturn(List.of(daily(stopped, 3)));

            DailyMedicationResponse response = service.getDailyMedication(USER_ID, TODAY);

            assertThat(response.medicines()).hasSize(1);
            assertThat(response.medicines().getFirst().stopped()).isTrue();
            assertThat(response.medicines().getFirst().dailyDoseCount()).isEqualTo(3);
            assertThat(response.medicines().getFirst().takenDoses()).containsExactly(1);
            assertThat(response.dailyTotalCount()).isZero();
            assertThat(response.dailyTakenCount()).isZero();
            verify(medicationLogRepository, never()).findLastTakenDates(anyCollection(), any());
        }
    }

    @Test
    @DisplayName("기간 조회 - from이 to보다 늦으면 INVALID_DATE_RANGE, 조회는 [from, to+1일)")
    void period() {
        assertThatThrownBy(() -> service.getMedicationLogsByPeriod(USER_ID, TODAY, TODAY.minusDays(1)))
                .extracting(MedicationLogServiceTest::errorCodeOf).isEqualTo(ErrorCode.INVALID_DATE_RANGE);

        given(medicationLogRepository.findTakenBetween(USER_ID, TODAY.minusDays(6), TODAY.plusDays(1))).willReturn(List.of());
        assertThat(service.getMedicationLogsByPeriod(USER_ID, TODAY.minusDays(6), TODAY)).isEmpty();
    }
}
