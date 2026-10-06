package com.gutlog.domain.record.meal.repository;

import com.gutlog.domain.record.meal.entity.MealFood;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MealFoodRepository extends JpaRepository<MealFood, Long> {
    @EntityGraph(attributePaths = "food")
    List<MealFood> findAllByMealRecordId(Long id);
}
