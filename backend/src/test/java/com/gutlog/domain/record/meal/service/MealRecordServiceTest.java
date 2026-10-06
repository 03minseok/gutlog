package com.gutlog.domain.record.meal.service;

import com.gutlog.domain.record.meal.dto.request.MealCreateRequest;
import com.gutlog.domain.record.meal.dto.response.MealResponse;
import com.gutlog.domain.record.meal.entity.Food;
import com.gutlog.domain.record.meal.entity.MealFood;
import com.gutlog.domain.record.meal.entity.MealRecord;
import com.gutlog.domain.record.meal.repository.FoodRepository;
import com.gutlog.domain.record.meal.repository.MealFoodRepository;
import com.gutlog.domain.record.meal.repository.MealRecordRepository;
import com.gutlog.domain.user.entity.IbdType;
import com.gutlog.domain.user.entity.Role;
import com.gutlog.domain.user.entity.User;
import com.gutlog.domain.user.repository.UserRepository;
import com.gutlog.global.exception.BusinessException;
import com.gutlog.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * MealRecordService 단위 테스트.
 * 레포지토리를 Mockito로 대체해서 소유권 검사, 음식 재사용/생성, 기간 계산 같은 서비스 로직만 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class MealRecordServiceTest {

    @Mock UserRepository userRepository;
    @Mock MealRecordRepository mealRecordRepository;
    @Mock FoodRepository foodRepository;
    @Mock MealFoodRepository mealFoodRepository;

    @InjectMocks MealRecordService mealRecordService;

    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;
    private static final LocalDateTime EATEN_AT = LocalDateTime.of(2026, 10, 6, 8, 30);

    private static User user(Long id) {
        User user = User.create("user" + id, "ENCODED", "닉" + id, Role.PATIENT, IbdType.ULCERATIVE_COLITIS);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private static MealRecord meal(Long id, User owner, LocalDateTime eatenAt) {
        MealRecord meal = MealRecord.create(owner, eatenAt);
        ReflectionTestUtils.setField(meal, "id", id);
        return meal;
    }

    private static Food food(User owner, String name) {
        return Food.create(owner, name);
    }

    private static ErrorCode errorCodeOf(Throwable e) {
        return ((BusinessException) e).getErrorCode();
    }

    @Nested
    @DisplayName("식사 등록")
    class Upload {

        @Test
        @DisplayName("성공 - 이미 있는 음식은 재사용하고, 없는 음식만 새로 만든다")
        void reuseExistingFood() {
            User user = user(USER_ID);
            Food rice = food(user, "밥");
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
            given(foodRepository.findByUserIdAndName(USER_ID, "밥")).willReturn(Optional.of(rice));
            given(foodRepository.findByUserIdAndName(USER_ID, "김치")).willReturn(Optional.empty());
            given(foodRepository.save(any(Food.class))).willAnswer(inv -> inv.getArgument(0));

            MealResponse response = mealRecordService.uploadMealRecord(
                    USER_ID, new MealCreateRequest(EATEN_AT, List.of("밥", "김치")));

            // 새 음식은 "김치" 하나만 저장
            ArgumentCaptor<Food> foodCaptor = ArgumentCaptor.forClass(Food.class);
            verify(foodRepository, times(1)).save(foodCaptor.capture());
            assertThat(foodCaptor.getValue().getName()).isEqualTo("김치");

            // 식사-음식 연결은 2개, 그중 "밥"은 기존 Food 객체를 그대로 사용
            ArgumentCaptor<MealFood> linkCaptor = ArgumentCaptor.forClass(MealFood.class);
            verify(mealFoodRepository, times(2)).save(linkCaptor.capture());
            assertThat(linkCaptor.getAllValues().get(0).getFood()).isSameAs(rice);
            assertThat(linkCaptor.getAllValues()).extracting(mf -> mf.getFood().getName())
                    .containsExactly("밥", "김치");

            verify(mealRecordRepository).save(any(MealRecord.class));
            assertThat(response.eatenAt()).isEqualTo(EATEN_AT);
            assertThat(response.foods()).containsExactly("밥", "김치");
        }

        @Test
        @DisplayName("실패 - 유저가 없으면 USER_NOT_FOUND, 아무것도 저장하지 않는다")
        void userNotFound() {
            given(userRepository.findById(USER_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> mealRecordService.uploadMealRecord(
                    USER_ID, new MealCreateRequest(EATEN_AT, List.of("밥"))))
                    .isInstanceOf(BusinessException.class)
                    .extracting(MealRecordServiceTest::errorCodeOf)
                    .isEqualTo(ErrorCode.USER_NOT_FOUND);

            verify(mealRecordRepository, never()).save(any());
            verify(mealFoodRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("단일 조회")
    class GetOne {

        @Test
        @DisplayName("성공 - 연결된 음식 이름을 함께 반환한다")
        void success() {
            User user = user(USER_ID);
            MealRecord meal = meal(10L, user, EATEN_AT);
            given(mealRecordRepository.findById(10L)).willReturn(Optional.of(meal));
            given(mealFoodRepository.findAllByMealRecordId(10L)).willReturn(List.of(
                    MealFood.of(food(user, "밥"), meal),
                    MealFood.of(food(user, "김치"), meal)));

            MealResponse response = mealRecordService.getMealRecord(USER_ID, 10L);

            assertThat(response.id()).isEqualTo(10L);
            assertThat(response.foods()).containsExactly("밥", "김치");
        }

        @Test
        @DisplayName("실패 - 없는 식사면 MEAL_RECORD_NOT_FOUND")
        void notFound() {
            given(mealRecordRepository.findById(10L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> mealRecordService.getMealRecord(USER_ID, 10L))
                    .extracting(MealRecordServiceTest::errorCodeOf)
                    .isEqualTo(ErrorCode.MEAL_RECORD_NOT_FOUND);
        }

        @Test
        @DisplayName("실패 - 남의 식사면 존재를 숨기고 MEAL_RECORD_NOT_FOUND")
        void notOwner() {
            given(mealRecordRepository.findById(10L)).willReturn(Optional.of(meal(10L, user(OTHER_USER_ID), EATEN_AT)));

            assertThatThrownBy(() -> mealRecordService.getMealRecord(USER_ID, 10L))
                    .extracting(MealRecordServiceTest::errorCodeOf)
                    .isEqualTo(ErrorCode.MEAL_RECORD_NOT_FOUND);

            verify(mealFoodRepository, never()).findAllByMealRecordId(any());
        }
    }

    @Nested
    @DisplayName("기간 조회")
    class GetByPeriod {

        @Test
        @DisplayName("from~to를 [from 00:00, to+1일 00:00) 범위로 바꿔서 조회한다")
        void rangeConversion() {
            given(mealRecordRepository.findAllByUserIdAndEatenAtGreaterThanEqualAndEatenAtLessThanOrderByEatenAtAsc(any(), any(), any()))
                    .willReturn(List.of());

            mealRecordService.getAllMealRecordByPeriod(USER_ID, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 6));

            verify(mealRecordRepository).findAllByUserIdAndEatenAtGreaterThanEqualAndEatenAtLessThanOrderByEatenAtAsc(
                    USER_ID,
                    LocalDateTime.of(2026, 10, 1, 0, 0),
                    LocalDateTime.of(2026, 10, 7, 0, 0));
        }

        @Test
        @DisplayName("식사가 없으면 빈 리스트, 음식 조회 쿼리는 날리지 않는다")
        void empty() {
            given(mealRecordRepository.findAllByUserIdAndEatenAtGreaterThanEqualAndEatenAtLessThanOrderByEatenAtAsc(any(), any(), any()))
                    .willReturn(List.of());

            List<MealResponse> result = mealRecordService.getAllMealRecordByPeriod(
                    USER_ID, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 1));

            assertThat(result).isEmpty();
            verify(mealFoodRepository, never()).findAllByMealRecordIdIn(anyList());
        }

        @Test
        @DisplayName("음식은 한 번의 IN 쿼리로 가져와서 식사별로 묶는다. 음식 연결이 없는 식사는 빈 목록")
        void groupFoodsByMeal() {
            User user = user(USER_ID);
            MealRecord breakfast = meal(10L, user, EATEN_AT);
            MealRecord lunch = meal(11L, user, EATEN_AT.plusHours(4));
            MealRecord snack = meal(12L, user, EATEN_AT.plusHours(7));
            given(mealRecordRepository.findAllByUserIdAndEatenAtGreaterThanEqualAndEatenAtLessThanOrderByEatenAtAsc(any(), any(), any()))
                    .willReturn(List.of(breakfast, lunch, snack));
            given(mealFoodRepository.findAllByMealRecordIdIn(List.of(10L, 11L, 12L))).willReturn(List.of(
                    MealFood.of(food(user, "밥"), breakfast),
                    MealFood.of(food(user, "국수"), lunch),
                    MealFood.of(food(user, "김치"), breakfast)));

            List<MealResponse> result = mealRecordService.getAllMealRecordByPeriod(
                    USER_ID, LocalDate.of(2026, 10, 6), LocalDate.of(2026, 10, 6));

            assertThat(result).extracting(MealResponse::id).containsExactly(10L, 11L, 12L);
            assertThat(result.get(0).foods()).containsExactly("밥", "김치");
            assertThat(result.get(1).foods()).containsExactly("국수");
            assertThat(result.get(2).foods()).isEmpty();
            verify(mealFoodRepository, times(1)).findAllByMealRecordIdIn(anyList());
        }

        @Test
        @DisplayName("from과 to가 같은 날이면 정상 조회")
        void sameDay() {
            given(mealRecordRepository.findAllByUserIdAndEatenAtGreaterThanEqualAndEatenAtLessThanOrderByEatenAtAsc(any(), any(), any()))
                    .willReturn(List.of());

            assertThat(mealRecordService.getAllMealRecordByPeriod(
                    USER_ID, LocalDate.of(2026, 10, 6), LocalDate.of(2026, 10, 6))).isEmpty();
        }

        @Test
        @DisplayName("실패 - from이 to보다 늦으면 INVALID_DATE_RANGE")
        void invalidRange() {
            assertThatThrownBy(() -> mealRecordService.getAllMealRecordByPeriod(
                    USER_ID, LocalDate.of(2026, 10, 7), LocalDate.of(2026, 10, 6)))
                    .extracting(MealRecordServiceTest::errorCodeOf)
                    .isEqualTo(ErrorCode.INVALID_DATE_RANGE);

            verify(mealRecordRepository, never())
                    .findAllByUserIdAndEatenAtGreaterThanEqualAndEatenAtLessThanOrderByEatenAtAsc(any(), any(), any());
        }
    }

    @Nested
    @DisplayName("삭제")
    class Delete {

        @Test
        @DisplayName("성공 - 음식 연결을 먼저 지우고 식사를 지운다 (FK 순서)")
        void success() {
            MealRecord meal = meal(10L, user(USER_ID), EATEN_AT);
            given(mealRecordRepository.findById(10L)).willReturn(Optional.of(meal));

            mealRecordService.deleteMealRecord(USER_ID, 10L);

            var order = inOrder(mealFoodRepository, mealRecordRepository);
            order.verify(mealFoodRepository).deleteAllByMealRecord(meal);
            order.verify(mealRecordRepository).delete(meal);
        }

        @Test
        @DisplayName("실패 - 남의 식사는 지우지 않는다")
        void notOwner() {
            given(mealRecordRepository.findById(10L)).willReturn(Optional.of(meal(10L, user(OTHER_USER_ID), EATEN_AT)));

            assertThatThrownBy(() -> mealRecordService.deleteMealRecord(USER_ID, 10L))
                    .extracting(MealRecordServiceTest::errorCodeOf)
                    .isEqualTo(ErrorCode.MEAL_RECORD_NOT_FOUND);

            verify(mealFoodRepository, never()).deleteAllByMealRecord(any());
            verify(mealRecordRepository, never()).delete(any());
        }
    }

    @Nested
    @DisplayName("수정")
    class Edit {

        @Test
        @DisplayName("성공 - 시간을 바꾸고, 기존 음식 연결을 지운 뒤 새로 연결한다")
        void success() {
            User user = user(USER_ID);
            MealRecord meal = meal(10L, user, EATEN_AT);
            LocalDateTime newTime = EATEN_AT.plusHours(1);
            given(mealRecordRepository.findById(10L)).willReturn(Optional.of(meal));
            given(foodRepository.findByUserIdAndName(USER_ID, "죽")).willReturn(Optional.empty());
            given(foodRepository.save(any(Food.class))).willAnswer(inv -> inv.getArgument(0));

            MealResponse response = mealRecordService.editMealRecord(
                    USER_ID, 10L, new MealCreateRequest(newTime, List.of("죽")));

            assertThat(meal.getEatenAt()).isEqualTo(newTime);
            var order = inOrder(mealFoodRepository);
            order.verify(mealFoodRepository).deleteAllByMealRecord(meal);
            order.verify(mealFoodRepository).save(any(MealFood.class));
            assertThat(response.eatenAt()).isEqualTo(newTime);
            assertThat(response.foods()).containsExactly("죽");
        }

        @Test
        @DisplayName("실패 - 남의 식사는 수정하지 않는다")
        void notOwner() {
            MealRecord meal = meal(10L, user(OTHER_USER_ID), EATEN_AT);
            given(mealRecordRepository.findById(10L)).willReturn(Optional.of(meal));

            assertThatThrownBy(() -> mealRecordService.editMealRecord(
                    USER_ID, 10L, new MealCreateRequest(EATEN_AT.plusHours(1), List.of("죽"))))
                    .extracting(MealRecordServiceTest::errorCodeOf)
                    .isEqualTo(ErrorCode.MEAL_RECORD_NOT_FOUND);

            assertThat(meal.getEatenAt()).isEqualTo(EATEN_AT);
            verify(mealFoodRepository, never()).deleteAllByMealRecord(any());
        }
    }
}
