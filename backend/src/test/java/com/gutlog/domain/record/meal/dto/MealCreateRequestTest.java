package com.gutlog.domain.record.meal.dto;

import com.gutlog.domain.record.meal.dto.request.MealCreateRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MealCreateRequest 테스트.
 * 생성자에서 하는 음식 이름 정규화(공백 제거, null 제거, 중복 제거)와 Bean Validation 규칙을 검증한다.
 */
class MealCreateRequestTest {

    private static final LocalDateTime EATEN_AT = LocalDateTime.of(2026, 10, 6, 8, 30);

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

    private Set<String> messages(MealCreateRequest request) {
        return validator.validate(request).stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toSet());
    }

    @Nested
    @DisplayName("음식 이름 정규화")
    class Normalize {

        @Test
        @DisplayName("앞뒤 공백을 제거한다")
        void strip() {
            MealCreateRequest request = new MealCreateRequest(EATEN_AT, List.of("  김치찌개 ", "\t밥"));
            assertThat(request.foods()).containsExactly("김치찌개", "밥");
        }

        @Test
        @DisplayName("공백 제거 후 같은 이름은 하나로 합치고 입력 순서를 유지한다")
        void distinct() {
            MealCreateRequest request = new MealCreateRequest(EATEN_AT, List.of("밥", "김치", " 밥 ", "김치"));
            assertThat(request.foods()).containsExactly("밥", "김치");
        }

        @Test
        @DisplayName("null 원소는 걸러낸다")
        void filterNull() {
            MealCreateRequest request = new MealCreateRequest(EATEN_AT, Arrays.asList("밥", null, "김치"));
            assertThat(request.foods()).containsExactly("밥", "김치");
        }

        @Test
        @DisplayName("foods 자체가 null이면 null 그대로 두고 검증에서 걸린다")
        void nullList() {
            MealCreateRequest request = new MealCreateRequest(EATEN_AT, null);
            assertThat(request.foods()).isNull();
            assertThat(messages(request)).contains("음식을 입력해주세요.");
        }
    }

    @Nested
    @DisplayName("검증 규칙")
    class Validate {

        @Test
        @DisplayName("정상 요청은 위반 사항이 없다")
        void valid() {
            assertThat(validator.validate(new MealCreateRequest(EATEN_AT, List.of("밥")))).isEmpty();
        }

        @Test
        @DisplayName("식사 시간이 없으면 실패")
        void eatenAtNull() {
            assertThat(messages(new MealCreateRequest(null, List.of("밥"))))
                    .containsExactly("식사 시간은 필수입니다.");
        }

        @Test
        @DisplayName("음식 목록이 비어 있으면 실패")
        void emptyFoods() {
            assertThat(messages(new MealCreateRequest(EATEN_AT, List.of())))
                    .containsExactly("음식을 입력해주세요.");
        }

        @Test
        @DisplayName("공백만 있는 음식 이름은 정규화 후 빈 문자열이 되어 실패")
        void blankFood() {
            assertThat(messages(new MealCreateRequest(EATEN_AT, List.of("밥", "   "))))
                    .containsExactly("음식을 입력해주세요.");
        }

        @Test
        @DisplayName("음식이 20개를 넘으면 실패")
        void tooManyFoods() {
            List<String> foods = IntStream.rangeClosed(1, 21).mapToObj(i -> "음식" + i).toList();
            assertThat(messages(new MealCreateRequest(EATEN_AT, foods)))
                    .containsExactly("음식은 최대 20개까지 입력 가능합니다.");
        }

        @Test
        @DisplayName("중복을 합친 뒤 20개 이하면 통과")
        void duplicatesDoNotCount() {
            List<String> foods = new ArrayList<>(Collections.nCopies(30, "밥"));
            assertThat(validator.validate(new MealCreateRequest(EATEN_AT, foods))).isEmpty();
        }

        @Test
        @DisplayName("음식 이름이 50자를 넘으면 실패")
        void foodNameTooLong() {
            assertThat(messages(new MealCreateRequest(EATEN_AT, List.of("가".repeat(51)))))
                    .containsExactly("음식은 최대 50자까지 입력 가능합니다.");
        }
    }
}
