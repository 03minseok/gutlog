package com.gutlog.domain.record.weight.dto;

import com.gutlog.domain.record.weight.dto.request.WeightRequest;
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

import java.math.BigDecimal;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * WeightRequest Bean Validation 규칙 테스트. (필수, 1.00 ~ 300.00, 소수점 둘째 자리까지)
 */
class WeightRequestTest {

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

    private static Set<String> messagesOf(BigDecimal weight) {
        return validator.validate(new WeightRequest(weight)).stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toSet());
    }

    @ParameterizedTest
    @ValueSource(strings = {"1.00", "1", "62.35", "62.3", "300", "300.00", "299.99"})
    @DisplayName("성공 - 범위 안, 소수점 두 자리 이하")
    void valid(String weight) {
        assertThat(messagesOf(new BigDecimal(weight))).isEmpty();
    }

    @Test
    @DisplayName("실패 - 체중이 없으면 안 된다")
    void required() {
        assertThat(messagesOf(null)).containsExactly("체중은 필수입니다.");
    }

    @ParameterizedTest
    @ValueSource(strings = {"0.99", "0", "-1"})
    @DisplayName("실패 - 1kg 미만")
    void tooSmall(String weight) {
        assertThat(messagesOf(new BigDecimal(weight))).contains("체중은 1kg 이상이어야 합니다.");
    }

    @ParameterizedTest
    @ValueSource(strings = {"300.01", "301", "999.99"})
    @DisplayName("실패 - 300kg 초과")
    void tooLarge(String weight) {
        assertThat(messagesOf(new BigDecimal(weight))).contains("체중은 300kg 이하여야 합니다.");
    }

    @ParameterizedTest
    @ValueSource(strings = {"62.345", "62.001"})
    @DisplayName("실패 - 소수점 셋째 자리 이상")
    void tooManyFractionDigits(String weight) {
        assertThat(messagesOf(new BigDecimal(weight))).containsExactly("체중은 소수점 둘째 자리까지 입력할 수 있습니다.");
    }
}
