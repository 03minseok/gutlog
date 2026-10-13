package com.gutlog.domain.record.medication.service;

import com.gutlog.domain.record.medication.dto.request.UserMedicineCreateRequest;
import com.gutlog.domain.record.medication.dto.request.UserMedicineUpdateRequest;
import com.gutlog.domain.record.medication.dto.response.UserMedicineResponse;
import com.gutlog.domain.record.medication.entity.ScheduleType;
import com.gutlog.domain.record.medication.entity.UserMedicine;
import com.gutlog.domain.record.medication.repository.UserMedicineRepository;
import com.gutlog.domain.record.medication.entity.DosageForm;
import com.gutlog.domain.record.medication.entity.Medicine;
import com.gutlog.domain.record.medication.entity.MedicineCategory;
import com.gutlog.domain.record.medication.repository.MedicineRepository;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserMedicineServiceTest {

    @Mock UserMedicineRepository userMedicineRepository;
    @Mock MedicineRepository medicineRepository;
    @Mock UserRepository userRepository;

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-06T03:00:00Z"), SEOUL); // 서울 12:00
    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;

    private UserMedicineService service;

    @BeforeEach
    void setUp() {
        service = new UserMedicineService(userMedicineRepository, medicineRepository, userRepository, CLOCK);
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
        UserMedicine um = UserMedicine.create(owner, medicine, ScheduleType.DAILY, 3, null, new BigDecimal("1"), "식후");
        ReflectionTestUtils.setField(um, "id", id);
        return um;
    }

    private static ErrorCode errorCodeOf(Throwable e) {
        return ((BusinessException) e).getErrorCode();
    }

    private static UserMedicineCreateRequest createRequest(BigDecimal doseAmount) {
        return new UserMedicineCreateRequest(10L, ScheduleType.DAILY, 3, null, doseAmount, " 식후 ");
    }

    @Nested
    @DisplayName("등록")
    class Create {

        @Test
        @DisplayName("성공 - 복용 방식과 복용량을 저장하고, 응답에 약 정보와 단위를 담는다")
        void success() {
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(user(USER_ID)));
            given(medicineRepository.findByIdAndActiveTrue(10L)).willReturn(Optional.of(medicine(10L, DosageForm.TABLET)));
            given(userMedicineRepository.existsByUserIdAndMedicineIdAndDeletedFalse(USER_ID, 10L)).willReturn(false);

            UserMedicineResponse response = service.createUserMedicine(USER_ID, createRequest(new BigDecimal("1.5")));

            verify(userMedicineRepository).save(any(UserMedicine.class));
            assertThat(response.scheduleType()).isEqualTo(ScheduleType.DAILY);
            assertThat(response.dailyDoseCount()).isEqualTo(3);
            assertThat(response.doseAmount()).isEqualByComparingTo("1.5");
            assertThat(response.doseMemo()).isEqualTo("식후");
            assertThat(response.medicine().doseUnit()).isEqualTo("정");
        }

        @Test
        @DisplayName("실패 - 비활성화된(또는 없는) 약이면 MEDICINE_NOT_FOUND")
        void inactiveMedicine() {
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(user(USER_ID)));
            given(medicineRepository.findByIdAndActiveTrue(10L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.createUserMedicine(USER_ID, createRequest(null)))
                    .extracting(UserMedicineServiceTest::errorCodeOf)
                    .isEqualTo(ErrorCode.MEDICINE_NOT_FOUND);
            verify(userMedicineRepository, never()).save(any());
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
        }

        @Test
        @DisplayName("내복액은 0.1mL 단위라 1.2mL는 통과")
        void oralLiquidDose() {
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(user(USER_ID)));
            given(medicineRepository.findByIdAndActiveTrue(10L)).willReturn(Optional.of(medicine(10L, DosageForm.ORAL_LIQUID)));

            assertThat(service.createUserMedicine(USER_ID, createRequest(new BigDecimal("1.2"))).medicine().doseUnit()).isEqualTo("mL");
        }
    }

    @Nested
    @DisplayName("수정")
    class Edit {

        @Test
        @DisplayName("성공 - 복용 방식을 바꾸면 맞지 않는 값은 비운다")
        void changeSchedule() {
            UserMedicine um = userMedicine(5L, user(USER_ID), medicine(10L, DosageForm.SC_INJECTION));
            given(userMedicineRepository.findById(5L)).willReturn(Optional.of(um));

            service.editUserMedicine(USER_ID, 5L, new UserMedicineUpdateRequest(ScheduleType.INTERVAL, null, 14, new BigDecimal("1"), null));

            assertThat(um.getScheduleType()).isEqualTo(ScheduleType.INTERVAL);
            assertThat(um.getDailyDoseCount()).isNull();
            assertThat(um.getIntervalDays()).isEqualTo(14);
            assertThat(um.getDoseMemo()).isNull();
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
            assertThat(others.getDailyDoseCount()).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("삭제")
    class Delete {

        @Test
        @DisplayName("성공 - 소프트 딜리트, 끊은 시각은 서울 시간 기준")
        void softDelete() {
            UserMedicine um = userMedicine(5L, user(USER_ID), medicine(10L, DosageForm.TABLET));
            given(userMedicineRepository.findById(5L)).willReturn(Optional.of(um));

            service.deleteUserMedicine(USER_ID, 5L);

            assertThat(um.isDeleted()).isTrue();
            assertThat(um.getDeletedAt()).isEqualTo(LocalDateTime.of(2026, 10, 6, 12, 0));
            verify(userMedicineRepository, never()).delete(any());
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
