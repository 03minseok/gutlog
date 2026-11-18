package com.gutlog.domain.record.sleep.dto;

import com.gutlog.domain.record.sleep.dto.request.SleepRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SleepRequest Bean Validation 규칙 테스트. (필수, 30분 ~ 24시간, 30분 단위)
 * 공통 예외 처리가 첫 오류 문구만 보여주므로, 한 값에 오류 문구가 정확히 하나만 나오는지도 확인한다.
 */
class SleepRequestTest {

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

    private static List<String> messagesOf(Integer sleepMinutes) {
        return validator.validate(new SleepRequest(sleepMinutes)).stream()
                .map(ConstraintViolation::getMessage)
                .toList();
    }

    @ParameterizedTest
    @ValueSource(ints = {30, 60, 450, 480, 1410, 1440})
    @DisplayName("성공 - 30분 ~ 24시간, 30분 단위")
    void valid(int sleepMinutes) {
        assertThat(messagesOf(sleepMinutes)).isEmpty();
    }

    @Test
    @DisplayName("실패 - 수면 시간이 없으면 안 된다")
    void required() {
        assertThat(messagesOf(null)).containsExactly("수면 시간은 필수입니다.");
    }

    @ParameterizedTest
    @ValueSource(ints = {29, 15, 0, -30})
    @DisplayName("실패 - 30분 미만 (30분 단위 오류는 함께 나오지 않는다)")
    void tooSmall(int sleepMinutes) {
        assertThat(messagesOf(sleepMinutes)).containsExactly("수면 시간은 30분 이상이어야 합니다.");
    }

    @ParameterizedTest
    @ValueSource(ints = {1441, 1470, 2000})
    @DisplayName("실패 - 24시간 초과 (30분 단위 오류는 함께 나오지 않는다)")
    void tooLarge(int sleepMinutes) {
        assertThat(messagesOf(sleepMinutes)).containsExactly("수면 시간은 24시간 이하여야 합니다.");
    }

    @ParameterizedTest
    @ValueSource(ints = {31, 45, 455, 1439})
    @DisplayName("실패 - 범위 안이지만 30분 단위가 아님")
    void notHalfHourUnit(int sleepMinutes) {
        assertThat(messagesOf(sleepMinutes)).containsExactly("수면 시간은 30분 단위로 입력해 주세요.");
    }
}
