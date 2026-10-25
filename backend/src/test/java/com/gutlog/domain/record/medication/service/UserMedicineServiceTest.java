package com.gutlog.domain.record.medication.service;

import com.gutlog.domain.record.medication.dto.request.UserMedicineCreateRequest;
import com.gutlog.domain.record.medication.dto.request.UserMedicineUpdateRequest;
import com.gutlog.domain.record.medication.dto.response.UserMedicineResponse;
import com.gutlog.domain.record.medication.entity.DosageForm;
import com.gutlog.domain.record.medication.entity.MedicationSchedule;
import com.gutlog.domain.record.medication.entity.Medicine;
import com.gutlog.domain.record.medication.entity.MedicineCategory;
import com.gutlog.domain.record.medication.entity.ScheduleType;
import com.gutlog.domain.record.medication.entity.UserMedicine;
import com.gutlog.domain.record.medication.repository.MedicationScheduleRepository;
import com.gutlog.domain.record.medication.repository.MedicineRepository;
import com.gutlog.domain.record.medication.repository.UserMedicineRepository;
import com.gutlog.domain.user.entity.IbdType;
import com.gutlog.domain.user.entity.Role;
import com.gutlog.domain.user.entity.User;
import com.gutlog.domain.user.repository.UserRepository;
import com.gutlog.global.exception.BusinessException;
import com.gutlog.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * UserMedicineService 단위 테스트. 오늘 = 2026-10-06 12:00 (서울)
 * 복용 방식·복용량은 기간별 일정(MedicationSchedule)에 둔다. 수정하면 오늘부터 새 일정, 끊으면 일정을 오늘로 닫는다.
 */
@ExtendWith(MockitoExtension.class)
class UserMedicineServiceTest {

    @Mock UserMedicineRepository userMedicineRepository;
    @Mock MedicationScheduleRepository scheduleRepository;
    @Mock MedicineRepository medicineRepository;
    @Mock UserRepository userRepository;

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-06T03:00:00Z"), SEOUL); // 서울 12:00
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 6);
    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;

    private UserMedicineService service;

    @BeforeEach
    void setUp() {
        service = new UserMedicineService(userMedicineRepository, scheduleRepository, medicineRepository, userRepository, CLOCK);
    }

    private static User user(Long id) {
        User user = User.create("user" + id, "ENCODED", "닉" + id, Role.PATIENT, IbdType.ULCERATIVE_COLITIS);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private static Medicine medicine(Long id, DosageForm form) {
        Medicine medicine = Medicine.create("메살라진", "펜타사", form, MedicineCategory.AMINOSALICYLATE);
        ReflectionTestUtils.setField(medicine, "id", id);
        return medicine;
    }

    private static UserMedicine userMedicine(Long id, User owner, Medicine medicine) {
        UserMedicine um = UserMedicine.create(owner, medicine);
        ReflectionTestUtils.setField(um, "id", id);
        return um;
    }

    private static MedicationSchedule dailySchedule(UserMedicine um, LocalDate from, int count) {
        return MedicationSchedule.create(um, from, ScheduleType.DAILY, count, null, new BigDecimal("1"), "식후");
    }

    private static ErrorCode errorCodeOf(Throwable e) {
        return ((BusinessException) e).getErrorCode();
    }

    private static UserMedicineCreateRequest createRequest(BigDecimal doseAmount) {
        return new UserMedicineCreateRequest(10L, ScheduleType.DAILY, 3, null, doseAmount, " 식후 ");
    }

    @Test
    @DisplayName("목록 - 끊지 않은 약의 현재 일정으로 응답을 만든다")
    void list() {
        UserMedicine um = userMedicine(5L, user(USER_ID), medicine(10L, DosageForm.TABLET));
        given(scheduleRepository.findCurrentByUserId(USER_ID)).willReturn(List.of(dailySchedule(um, TODAY.minusDays(3), 2)));

        List<UserMedicineResponse> result = service.getMyMedicines(USER_ID);

        assertThat(result).singleElement().satisfies(r -> {
            assertThat(r.id()).isEqualTo(5L);
            assertThat(r.dailyDoseCount()).isEqualTo(2);
            assertThat(r.medicine().brandName()).isEqualTo("펜타사");
        });
    }

    @Nested
    @DisplayName("등록")
    class Create {

        @Test
        @DisplayName("성공 - 복용 약과 오늘부터 시작하는 첫 일정을 함께 저장한다")
        void success() {
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(user(USER_ID)));
            given(medicineRepository.findByIdAndActiveTrue(10L)).willReturn(Optional.of(medicine(10L, DosageForm.TABLET)));
            given(userMedicineRepository.existsByUserIdAndMedicineIdAndDeletedFalse(USER_ID, 10L)).willReturn(false);
            given(userMedicineRepository.save(any(UserMedicine.class))).willAnswer(inv -> inv.getArgument(0));
            given(scheduleRepository.save(any(MedicationSchedule.class))).willAnswer(inv -> inv.getArgument(0));

            UserMedicineResponse response = service.createUserMedicine(USER_ID, createRequest(new BigDecimal("1.5")));

            ArgumentCaptor<MedicationSchedule> captor = ArgumentCaptor.forClass(MedicationSchedule.class);
            verify(scheduleRepository).save(captor.capture());
            MedicationSchedule schedule = captor.getValue();
            assertThat(schedule.getEffectiveFrom()).isEqualTo(TODAY);
            assertThat(schedule.getEffectiveTo()).isNull();
            assertThat(schedule.getDailyDoseCount()).isEqualTo(3);
            assertThat(schedule.getDoseMemo()).isEqualTo("식후");
            assertThat(response.scheduleType()).isEqualTo(ScheduleType.DAILY);
            assertThat(response.doseAmount()).isEqualByComparingTo("1.5");
            assertThat(response.medicine().doseUnit()).isEqualTo("정");
        }

        @Test
        @DisplayName("실패 - 비활성화된(또는 없는) 약이면 MEDICINE_NOT_FOUND, 아무것도 저장하지 않는다")
        void inactiveMedicine() {
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(user(USER_ID)));
            given(medicineRepository.findByIdAndActiveTrue(10L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.createUserMedicine(USER_ID, createRequest(null)))
                    .extracting(UserMedicineServiceTest::errorCodeOf)
                    .isEqualTo(ErrorCode.MEDICINE_NOT_FOUND);
            verify(userMedicineRepository, never()).save(any());
            verify(scheduleRepository, never()).save(any());
        }

        @Test
        @DisplayName("실패 - 같은 약을 이미 복용 중이면 DUPLICATE_USER_MEDICINE")
        void duplicate() {
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(user(USER_ID)));
            given(medicineRepository.findByIdAndActiveTrue(10L)).willReturn(Optional.of(medicine(10L, DosageForm.TABLET)));
            given(userMedicineRepository.existsByUserIdAndMedicineIdAndDeletedFalse(USER_ID, 10L)).willReturn(true);

            assertThatThrownBy(() -> service.createUserMedicine(USER_ID, createRequest(null)))
                    .extracting(UserMedicineServiceTest::errorCodeOf)
                    .isEqualTo(ErrorCode.DUPLICATE_USER_MEDICINE);
            verify(userMedicineRepository, never()).save(any());
        }

        @Test
        @DisplayName("실패 - 제형의 단위에 맞지 않는 복용량이면 INVALID_DOSE_AMOUNT (정제 1.2정)")
        void invalidDoseAmount() {
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(user(USER_ID)));
            given(medicineRepository.findByIdAndActiveTrue(10L)).willReturn(Optional.of(medicine(10L, DosageForm.TABLET)));

            assertThatThrownBy(() -> service.createUserMedicine(USER_ID, createRequest(new BigDecimal("1.2"))))
                    .extracting(UserMedicineServiceTest::errorCodeOf)
                    .isEqualTo(ErrorCode.INVALID_DOSE_AMOUNT);
            verify(userMedicineRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("수정 - 일정 이력")
    class Edit {

        private static final UserMedicineUpdateRequest TO_INTERVAL =
                new UserMedicineUpdateRequest(ScheduleType.INTERVAL, null, 14, new BigDecimal("1"), null);

        @Test
        @DisplayName("예전에 시작한 일정이면 오늘로 닫고, 오늘부터 새 일정을 만든다 (닫기를 먼저 반영)")
        void closeAndCreate() {
            UserMedicine um = userMedicine(5L, user(USER_ID), medicine(10L, DosageForm.SC_INJECTION));
            MedicationSchedule current = dailySchedule(um, TODAY.minusDays(10), 3);
            given(userMedicineRepository.findById(5L)).willReturn(Optional.of(um));
            given(scheduleRepository.findCurrent(5L)).willReturn(Optional.of(current));
            given(scheduleRepository.save(any(MedicationSchedule.class))).willAnswer(inv -> inv.getArgument(0));

            UserMedicineResponse response = service.editUserMedicine(USER_ID, 5L, TO_INTERVAL);

            assertThat(current.getEffectiveTo()).isEqualTo(TODAY);
            assertThat(current.getDailyDoseCount()).isEqualTo(3);   // 지난 일정 값은 그대로

            ArgumentCaptor<MedicationSchedule> captor = ArgumentCaptor.forClass(MedicationSchedule.class);
            InOrder order = inOrder(scheduleRepository);
            order.verify(scheduleRepository).flush();
            order.verify(scheduleRepository).save(captor.capture());
            MedicationSchedule next = captor.getValue();
            assertThat(next.getEffectiveFrom()).isEqualTo(TODAY);
            assertThat(next.getEffectiveTo()).isNull();
            assertThat(next.getScheduleType()).isEqualTo(ScheduleType.INTERVAL);
            assertThat(next.getDailyDoseCount()).isNull();
            assertThat(next.getIntervalDays()).isEqualTo(14);
            assertThat(response.intervalDays()).isEqualTo(14);
        }

        @Test
        @DisplayName("오늘 시작한 일정이면 새로 만들지 않고 그 일정을 고친다")
        void sameDayOverwrite() {
            UserMedicine um = userMedicine(5L, user(USER_ID), medicine(10L, DosageForm.SC_INJECTION));
            MedicationSchedule current = dailySchedule(um, TODAY, 3);
            given(userMedicineRepository.findById(5L)).willReturn(Optional.of(um));
            given(scheduleRepository.findCurrent(5L)).willReturn(Optional.of(current));

            service.editUserMedicine(USER_ID, 5L, TO_INTERVAL);

            assertThat(current.getEffectiveTo()).isNull();
            assertThat(current.getScheduleType()).isEqualTo(ScheduleType.INTERVAL);
            assertThat(current.getIntervalDays()).isEqualTo(14);
            assertThat(current.getDoseMemo()).isNull();
            verify(scheduleRepository, never()).save(any());
        }

        @Test
        @DisplayName("실패 - 남의 약이면 404, 끊은 약도 404")
        void notMineOrStopped() {
            UserMedicine others = userMedicine(5L, user(OTHER_USER_ID), medicine(10L, DosageForm.TABLET));
            UserMedicine stopped = userMedicine(6L, user(USER_ID), medicine(10L, DosageForm.TABLET));
            stopped.delete(LocalDateTime.now(CLOCK));
            given(userMedicineRepository.findById(5L)).willReturn(Optional.of(others));
            given(userMedicineRepository.findById(6L)).willReturn(Optional.of(stopped));
            UserMedicineUpdateRequest request = new UserMedicineUpdateRequest(ScheduleType.DAILY, 1, null, null, null);

            assertThatThrownBy(() -> service.editUserMedicine(USER_ID, 5L, request))
                    .extracting(UserMedicineServiceTest::errorCodeOf).isEqualTo(ErrorCode.USER_MEDICINE_NOT_FOUND);
            assertThatThrownBy(() -> service.editUserMedicine(USER_ID, 6L, request))
                    .extracting(UserMedicineServiceTest::errorCodeOf).isEqualTo(ErrorCode.USER_MEDICINE_NOT_FOUND);
            verify(scheduleRepository, never()).findCurrent(any());
        }

        @Test
        @DisplayName("실패 - 현재 일정이 없으면 SCHEDULE_NOT_FOUND")
        void noCurrentSchedule() {
            UserMedicine um = userMedicine(5L, user(USER_ID), medicine(10L, DosageForm.TABLET));
            given(userMedicineRepository.findById(5L)).willReturn(Optional.of(um));
            given(scheduleRepository.findCurrent(5L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.editUserMedicine(USER_ID, 5L, new UserMedicineUpdateRequest(ScheduleType.AS_NEEDED, null, null, null, null)))
                    .extracting(UserMedicineServiceTest::errorCodeOf).isEqualTo(ErrorCode.SCHEDULE_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("삭제")
    class Delete {

        @Test
        @DisplayName("성공 - 소프트 딜리트하고 현재 일정을 오늘로 닫는다 (서울 시간 기준)")
        void softDeleteAndClose() {
            UserMedicine um = userMedicine(5L, user(USER_ID), medicine(10L, DosageForm.TABLET));
            MedicationSchedule current = dailySchedule(um, TODAY.minusDays(10), 2);
            given(userMedicineRepository.findById(5L)).willReturn(Optional.of(um));
            given(scheduleRepository.findCurrent(5L)).willReturn(Optional.of(current));

            service.deleteUserMedicine(USER_ID, 5L);

            assertThat(um.isDeleted()).isTrue();
            assertThat(um.getDeletedAt()).isEqualTo(LocalDateTime.of(2026, 10, 6, 12, 0));
            assertThat(current.getEffectiveTo()).isEqualTo(TODAY);
            verify(userMedicineRepository, never()).delete(any());
            verify(scheduleRepository, never()).delete(any());
        }

        @Test
        @DisplayName("실패 - 이미 끊은 약을 또 지우면 404")
        void alreadyStopped() {
            UserMedicine um = userMedicine(5L, user(USER_ID), medicine(10L, DosageForm.TABLET));
            um.delete(LocalDateTime.now(CLOCK));
            given(userMedicineRepository.findById(5L)).willReturn(Optional.of(um));

            assertThatThrownBy(() -> service.deleteUserMedicine(USER_ID, 5L))
                    .extracting(UserMedicineServiceTest::errorCodeOf).isEqualTo(ErrorCode.USER_MEDICINE_NOT_FOUND);
        }
    }
}
