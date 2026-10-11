package com.gutlog.domain.record.bowel.dto;

import com.gutlog.domain.record.bowel.dto.request.BowelCreateRequest;
import com.gutlog.domain.record.bowel.entity.BristolType;
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

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * BowelCreateRequest Bean Validation 규칙 테스트.
 */
class BowelCreateRequestTest {

    private static final LocalDateTime BOWEL_AT = LocalDateTime.of(2026, 10, 6, 8, 30);

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

    private Set<String> messages(BowelCreateRequest request) {
        return validator.validate(request).stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toSet());
    }

    private static BowelCreateRequest withPain(Integer painLevel) {
        return new BowelCreateRequest(BOWEL_AT, false, BristolType.TYPE_4, painLevel, null);
    }

    private static BowelCreateRequest withMemo(String memo) {
        return new BowelCreateRequest(BOWEL_AT, false, BristolType.TYPE_4, 0, memo);
    }

    @Test
    @DisplayName("정상 요청은 위반 사항이 없다 (메모는 null 허용)")
    void valid() {
        assertThat(validator.validate(withMemo(null))).isEmpty();
        assertThat(validator.validate(withMemo("메모"))).isEmpty();
    }

    @Test
    @DisplayName("배변 시간이 없으면 실패")
    void bowelAtNull() {
        assertThat(messages(new BowelCreateRequest(null, false, BristolType.TYPE_4, 0, null)))
                .containsExactly("배변 시간은 필수입니다.");
    }

    @Test
    @DisplayName("배변 상태가 없으면 실패")
    void bowelConditionNull() {
        assertThat(messages(new BowelCreateRequest(BOWEL_AT, false, null, 0, null)))
                .containsExactly("배변 상태는 필수입니다.");
    }

    @Test
    @DisplayName("통증 정도가 없으면 실패")
    void painLevelNull() {
        assertThat(messages(withPain(null))).containsExactly("통증 정도는 필수입니다.");
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 5, 10})
    @DisplayName("통증 정도 경계값 0~10은 통과")
    void painLevelInRange(int painLevel) {
        assertThat(validator.validate(withPain(painLevel))).isEmpty();
    }

    @Test
    @DisplayName("통증 정도가 0 미만이면 실패")
    void painLevelTooLow() {
        assertThat(messages(withPain(-1))).containsExactly("통증은 0 이상이어야 합니다.");
    }

    @Test
    @DisplayName("통증 정도가 10 초과면 실패")
    void painLevelTooHigh() {
        assertThat(messages(withPain(11))).containsExactly("통증은 10 이하여야 합니다.");
    }

    @Test
    @DisplayName("메모는 500자까지 통과, 501자면 실패")
    void memoLength() {
        assertThat(validator.validate(withMemo("가".repeat(500)))).isEmpty();
        assertThat(messages(withMemo("가".repeat(501))))
                .containsExactly("메모는 최대 500자까지 입력 가능합니다.");
    }
}
