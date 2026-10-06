package com.gutlog.domain.record.meal.repository;

import com.gutlog.domain.record.meal.entity.Food;
import com.gutlog.domain.record.meal.entity.MealFood;
import com.gutlog.domain.record.meal.entity.MealRecord;
import com.gutlog.domain.user.entity.IbdType;
import com.gutlog.domain.user.entity.Role;
import com.gutlog.domain.user.entity.User;
import com.gutlog.domain.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * meal 도메인 레포지토리 슬라이스 테스트.
 * 쿼리 메서드 이름으로 만든 조건(기간 경계, 정렬, 유저 필터)과 unique 제약을 실제 H2에서 확인한다.
 */
@DataJpaTest
class MealRepositoryTest {

    @Autowired UserRepository userRepository;
    @Autowired MealRecordRepository mealRecordRepository;
    @Autowired FoodRepository foodRepository;
    @Autowired MealFoodRepository mealFoodRepository;
    @Autowired EntityManager em;

    private User me;
    private User other;

    @BeforeEach
    void setUp() {
        me = userRepository.save(User.create("me", "ENCODED", "나", Role.PATIENT, IbdType.ULCERATIVE_COLITIS));
        other = userRepository.save(User.create("other", "ENCODED", "남", Role.GUARDIAN, null));
    }

    private MealRecord saveMeal(User user, LocalDateTime eatenAt) {
        return mealRecordRepository.save(MealRecord.create(user, eatenAt));
    }

    @Test
    @DisplayName("기간 조회 - 시작은 포함, 끝은 제외하고 시간순으로 정렬한다")
    void findByPeriod() {
        LocalDateTime start = LocalDateTime.of(2026, 10, 6, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 10, 7, 0, 0);

        MealRecord dinner = saveMeal(me, LocalDateTime.of(2026, 10, 6, 19, 0));
        MealRecord atStart = saveMeal(me, start);                          // 포함
        MealRecord lastMinute = saveMeal(me, end.minusNanos(1_000));       // 포함
        saveMeal(me, end);                                                 // 제외 (다음날 0시)
        saveMeal(me, start.minusSeconds(1));                               // 제외 (전날)
        saveMeal(other, LocalDateTime.of(2026, 10, 6, 12, 0));             // 제외 (남의 식사)

        List<MealRecord> result = mealRecordRepository
                .findAllByUserIdAndEatenAtGreaterThanEqualAndEatenAtLessThanOrderByEatenAtAsc(me.getId(), start, end);

        assertThat(result).extracting(MealRecord::getId)
                .containsExactly(atStart.getId(), dinner.getId(), lastMinute.getId());
    }

    @Test
    @DisplayName("음식 이름은 유저별로 unique - 같은 유저가 같은 이름을 두 번 저장하면 실패")
    void foodUniquePerUser() {
        foodRepository.saveAndFlush(Food.create(me, "밥"));

        assertThatThrownBy(() -> foodRepository.saveAndFlush(Food.create(me, "밥")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("다른 유저는 같은 음식 이름을 각자 가질 수 있다")
    void foodSameNameDifferentUsers() {
        foodRepository.saveAndFlush(Food.create(me, "밥"));
        foodRepository.saveAndFlush(Food.create(other, "밥"));

        assertThat(foodRepository.findByUserIdAndName(me.getId(), "밥")).isPresent();
        assertThat(foodRepository.findByUserIdAndName(other.getId(), "밥")).isPresent();
        assertThat(foodRepository.findByUserIdAndName(me.getId(), "김치")).isEmpty();
    }

    @Test
    @DisplayName("findAllByMealRecordIdIn - 여러 식사의 음식 연결을 한 번에 가져오고 food도 함께 로딩된다")
    void findAllByMealRecordIdIn() {
        MealRecord m1 = saveMeal(me, LocalDateTime.of(2026, 10, 6, 8, 0));
        MealRecord m2 = saveMeal(me, LocalDateTime.of(2026, 10, 6, 12, 0));
        MealRecord m3 = saveMeal(me, LocalDateTime.of(2026, 10, 6, 18, 0));
        Food rice = foodRepository.save(Food.create(me, "밥"));
        Food kimchi = foodRepository.save(Food.create(me, "김치"));
        mealFoodRepository.save(MealFood.of(rice, m1));
        mealFoodRepository.save(MealFood.of(kimchi, m1));
        mealFoodRepository.save(MealFood.of(rice, m2));
        mealFoodRepository.save(MealFood.of(rice, m3));
        em.flush();
        em.clear(); // 1차 캐시를 비워서 실제 쿼리 결과만 보도록

        List<MealFood> result = mealFoodRepository.findAllByMealRecordIdIn(List.of(m1.getId(), m2.getId()));

        assertThat(result).hasSize(3);
        // @EntityGraph로 food를 같이 가져왔으므로 프록시가 아닌 초기화된 상태여야 한다
        assertThat(result).allSatisfy(mf ->
                assertThat(em.getEntityManagerFactory().getPersistenceUnitUtil().isLoaded(mf.getFood())).isTrue());
    }

    @Test
    @DisplayName("같은 식사에 같은 음식을 두 번 연결하면 unique 제약 위반")
    void mealFoodUnique() {
        MealRecord meal = saveMeal(me, LocalDateTime.of(2026, 10, 6, 8, 0));
        Food rice = foodRepository.save(Food.create(me, "밥"));
        mealFoodRepository.saveAndFlush(MealFood.of(rice, meal));

        assertThatThrownBy(() -> mealFoodRepository.saveAndFlush(MealFood.of(rice, meal)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("deleteAllByMealRecord - 해당 식사의 연결만 지우고 음식(Food)과 다른 식사는 남긴다")
    void deleteAllByMealRecord() {
        MealRecord target = saveMeal(me, LocalDateTime.of(2026, 10, 6, 8, 0));
        MealRecord keep = saveMeal(me, LocalDateTime.of(2026, 10, 6, 12, 0));
        Food rice = foodRepository.save(Food.create(me, "밥"));
        mealFoodRepository.save(MealFood.of(rice, target));
        mealFoodRepository.save(MealFood.of(rice, keep));
        em.flush();

        mealFoodRepository.deleteAllByMealRecord(target);
        em.clear();

        assertThat(mealFoodRepository.findAllByMealRecordId(target.getId())).isEmpty();
        assertThat(mealFoodRepository.findAllByMealRecordId(keep.getId())).hasSize(1);
        assertThat(foodRepository.findById(rice.getId())).isPresent();
    }
}
