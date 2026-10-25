package com.gutlog.domain.record.medication.dto;

import com.gutlog.domain.record.medication.dto.request.UserMedicineCreateRequest;
import com.gutlog.domain.record.medication.dto.request.UserMedicineUpdateRequest;
import com.gutlog.domain.record.medication.entity.ScheduleType;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 복용 약 요청 검증. 복용 방식별 조합 규칙(DoseScheduleRequest)이 등록·수정 요청 모두에 걸리는지 확인한다.
 */
class UserMedicineRequestValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    private static Set<String> messages(Object request) {
        return validator.validate(request).stream().map(ConstraintViolation::getMessage).collect(Collectors.toSet());
    }

    private static UserMedicineCreateRequest create(ScheduleType type, Integer dailyDoseCount, Integer intervalDays) {
        return new UserMedicineCreateRequest(1L, type, dailyDoseCount, intervalDays, new BigDecimal("1"), "식후");
    }

    @Nested
    @DisplayName("복용 방식별 조합")
    class Schedule {

        @Test
        @DisplayName("올바른 조합은 통과")
        void valid() {
            assertThat(validator.validate(create(ScheduleType.DAILY, 3, null))).isEmpty();
            assertThat(validator.validate(create(ScheduleType.INTERVAL, null, 14))).isEmpty();
            assertThat(validator.validate(create(ScheduleType.AS_NEEDED, null, null))).isEmpty();
        }

        @Test
        @DisplayName("DAILY인데 하루 복용 횟수가 없으면 실패")
        void dailyWithoutCount() {
            assertThat(messages(create(ScheduleType.DAILY, null, null))).containsExactly("하루 복용 횟수를 입력해주세요.");
        }

        @Test
        @DisplayName("INTERVAL인데 투여 간격이 없으면 실패")
        void intervalWithoutDays() {
            assertThat(messages(create(ScheduleType.INTERVAL, null, null))).containsExactly("투여 간격을 입력해주세요.");
        }

        @Test
        @DisplayName("맞지 않는 값이 오면 무시하지 않고 실패")
        void wrongFields() {
            assertThat(messages(create(ScheduleType.AS_NEEDED, 3, null)))
                    .containsExactly("하루 복용 횟수는 매일 먹는 약에만 입력할 수 있습니다.");
            assertThat(messages(create(ScheduleType.AS_NEEDED, null, 14)))
                    .containsExactly("투여 간격은 정해진 간격으로 맞는 약에만 입력할 수 있습니다.");
            assertThat(messages(create(ScheduleType.DAILY, 2, 14)))
                    .containsExactly("투여 간격은 정해진 간격으로 맞는 약에만 입력할 수 있습니다.");
            assertThat(messages(create(ScheduleType.INTERVAL, 2, 14)))
                    .containsExactly("하루 복용 횟수는 매일 먹는 약에만 입력할 수 있습니다.");
        }

        @Test
        @DisplayName("복용 방식이 없으면 그 메시지 하나만 나온다")
        void noScheduleType() {
            assertThat(messages(create(null, 3, null))).containsExactly("복용 방식은 필수입니다.");
        }

        @Test
        @DisplayName("수정 요청에도 같은 규칙이 걸린다")
        void updateRequest() {
            assertThat(messages(new UserMedicineUpdateRequest(ScheduleType.DAILY, null, null, null, null)))
                    .containsExactly("하루 복용 횟수를 입력해주세요.");
            assertThat(messages(new UserMedicineUpdateRequest(ScheduleType.AS_NEEDED, 2, null, null, null)))
                    .containsExactly("하루 복용 횟수는 매일 먹는 약에만 입력할 수 있습니다.");
            assertThat(validator.validate(new UserMedicineUpdateRequest(ScheduleType.INTERVAL, null, 56, null, null))).isEmpty();
        }
    }

    @Nested
    @DisplayName("범위")
    class Range {

        @Test
        @DisplayName("하루 복용 횟수는 1~6")
        void dailyDoseCount() {
            assertThat(validator.validate(create(ScheduleType.DAILY, 1, null))).isEmpty();
            assertThat(validator.validate(create(ScheduleType.DAILY, 6, null))).isEmpty();
            assertThat(messages(create(ScheduleType.DAILY, 0, null))).containsExactly("하루 복용 횟수는 1~6회입니다.");
            assertThat(messages(create(ScheduleType.DAILY, 7, null))).containsExactly("하루 복용 횟수는 1~6회입니다.");
        }

        @Test
        @DisplayName("투여 간격은 2~180일")
        void intervalDays() {
            assertThat(validator.validate(create(ScheduleType.INTERVAL, null, 2))).isEmpty();
            assertThat(validator.validate(create(ScheduleType.INTERVAL, null, 180))).isEmpty();
            assertThat(messages(create(ScheduleType.INTERVAL, null, 1))).containsExactly("투여 간격은 2~180일입니다.");
            assertThat(messages(create(ScheduleType.INTERVAL, null, 181))).containsExactly("투여 간격은 2~180일입니다.");
        }

        @Test
        @DisplayName("약 선택은 필수, 복용량·메모는 선택")
        void requiredAndOptional() {
            assertThat(messages(new UserMedicineCreateRequest(null, ScheduleType.AS_NEEDED, null, null, null, null)))
                    .containsExactly("약을 선택해주세요.");
            assertThat(validator.validate(new UserMedicineCreateRequest(1L, ScheduleType.AS_NEEDED, null, null, null, null))).isEmpty();
        }

        @Test
        @DisplayName("복용량은 소수점 한 자리까지, 메모는 20자까지")
        void doseAmountAndMemo() {
            assertThat(messages(new UserMedicineCreateRequest(1L, ScheduleType.AS_NEEDED, null, null, new BigDecimal("1.25"), null)))
                    .containsExactly("복용량은 소수점 한 자리까지 입력할 수 있습니다.");
            assertThat(messages(new UserMedicineCreateRequest(1L, ScheduleType.AS_NEEDED, null, null, new BigDecimal("0"), null)))
                    .containsExactly("복용량이 올바르지 않습니다.");
            assertThat(messages(new UserMedicineCreateRequest(1L, ScheduleType.AS_NEEDED, null, null, null, "가".repeat(21))))
                    .containsExactly("복용 메모는 20자 이하로 입력해주세요.");
        }
    }
}
