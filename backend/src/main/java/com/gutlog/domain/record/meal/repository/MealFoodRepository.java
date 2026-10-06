package com.gutlog.domain.record.meal.repository;

import com.gutlog.domain.record.meal.entity.MealFood;
import com.gutlog.domain.record.meal.entity.MealRecord;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface MealFoodRepository extends JpaRepository<MealFood, Long> {
    @EntityGraph(attributePaths = "food")
    List<MealFood> findAllByMealRecordId(Long mealRecordId);

    @EntityGraph(attributePaths = "food")
    List<MealFood> findAllByMealRecordIdIn(List<Long> mealRecordIds);

    void deleteAllByMealRecord(MealRecord mealRecord);

}
