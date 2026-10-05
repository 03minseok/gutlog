package com.gutlog.domain.record.meal.repository;

import com.gutlog.domain.record.meal.entity.MealRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MealRecordRepository extends JpaRepository<MealRecord, Long> {
}
