package com.gutlog.domain.record.meal.repository;

import com.gutlog.domain.record.meal.entity.MealFood;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MealFoodRepository extends JpaRepository<MealFood, Long> {
    //todo: n+1 문제 해결
    List<MealFood> findAllByMealRecordId(Long id);
}
