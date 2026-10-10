package com.gutlog.domain.record.meal.service;

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
import jakarta.persistence.EntityManager;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 기간 조회의 실제 SQL 개수를 센다. (N+1 방지 회귀 테스트)
 *
 * Mockito 단위 테스트는 "레포지토리 메서드를 몇 번 불렀는지"만 알 수 있어서,
 * 지연 로딩 때문에 식사마다 select가 추가로 나가는 문제는 잡지 못한다.
 * 여기서는 Hibernate 통계(generate_statistics)를 테스트에서만 켜고 DB로 나간 문장 수를 직접 확인한다.
 */
@DataJpaTest
@Import(MealRecordService.class)
@TestPropertySource(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
class MealRecordServiceQueryCountTest {

    private static final LocalDate DAY = LocalDate.of(2026, 10, 6);

    @Autowired MealRecordService mealRecordService;
    @Autowired UserRepository userRepository;
    @Autowired MealRecordRepository mealRecordRepository;
    @Autowired FoodRepository foodRepository;
    @Autowired MealFoodRepository mealFoodRepository;
    @Autowired EntityManager em;

    private Statistics statistics;
    private User user;

    @BeforeEach
    void setUp() {
        statistics = em.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
        user = userRepository.save(User.create("counter", "ENCODED", "카운터", Role.PATIENT, IbdType.ULCERATIVE_COLITIS));
    }

    /** 식사 mealCount개, 식사마다 음식 3개(공유 음식 포함)를 저장한다. */
    private void saveMeals(int mealCount) {
        Food rice = foodRepository.save(Food.create(user, "밥"));
        for (int i = 0; i < mealCount; i++) {
            MealRecord meal = mealRecordRepository.save(MealRecord.create(user, DAY.atTime(7, 0).plusMinutes(30L * i)));
            mealFoodRepository.save(MealFood.of(rice, meal));
            mealFoodRepository.save(MealFood.of(foodRepository.save(Food.create(user, "반찬" + i)), meal));
            mealFoodRepository.save(MealFood.of(foodRepository.save(Food.create(user, "국" + i)), meal));
        }
    }

    /** 1차 캐시를 비우고 통계를 초기화해서, 이후 조회가 전부 실제 DB 쿼리로 세어지게 한다. */
    private void resetPersistenceContextAndStatistics() {
        em.flush();
        em.clear();
        statistics.clear();
    }

    @ParameterizedTest(name = "식사 {0}개")
    @ValueSource(ints = {1, 5, 20})
    @DisplayName("식사 개수와 상관없이 select는 2번 (식사 목록 1번 + 음식 연결·음식 1번)")
    void periodQueryRunsTwoSelects(int mealCount) {
        saveMeals(mealCount);
        resetPersistenceContextAndStatistics();

        List<MealResponse> result = mealRecordService.getAllMealRecordByPeriod(user.getId(), DAY, DAY);

        assertThat(result).hasSize(mealCount);
        // 응답에 음식 이름이 다 채워졌다 = food까지 실제로 읽었다. 그런데도 쿼리는 2번이어야 한다.
        assertThat(result).allSatisfy(meal -> assertThat(meal.foods()).hasSize(3).contains("밥"));
        assertThat(statistics.getPrepareStatementCount())
                .as("실행된 SQL 수 (N+1이면 식사 수만큼 늘어난다)")
                .isEqualTo(2);
    }

    @Test
    @DisplayName("기간 안에 식사가 없으면 select 1번 (음식 조회 생략)")
    void emptyPeriodRunsOneSelect() {
        saveMeals(3);
        resetPersistenceContextAndStatistics();

        List<MealResponse> result = mealRecordService.getAllMealRecordByPeriod(
                user.getId(), DAY.plusDays(1), DAY.plusDays(1));

        assertThat(result).isEmpty();
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("단건 조회는 select 2번 (식사 1번 + 음식 연결·음식 1번)")
    void singleQueryRunsTwoSelects() {
        saveMeals(1);
        Long mealId = mealRecordRepository.findAll().getFirst().getId();
        resetPersistenceContextAndStatistics();

        MealResponse result = mealRecordService.getMealRecord(user.getId(), mealId);

        assertThat(result.foods()).hasSize(3);
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(2);
    }
}
