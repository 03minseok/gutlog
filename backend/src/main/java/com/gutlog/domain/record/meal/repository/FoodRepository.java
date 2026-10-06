package com.gutlog.domain.record.meal.repository;

import com.gutlog.domain.record.meal.entity.Food;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FoodRepository extends JpaRepository<Food, Long> {
    Optional<Food> findByUserIdAndName(Long userId, String name);
}
