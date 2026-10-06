package com.gutlog.domain.record.meal.repository;

import com.gutlog.domain.record.meal.entity.MealRecord;
import com.gutlog.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MealRecordRepository extends JpaRepository<MealRecord, Long> {
	List<MealRecord> findAllByUserIdAndEatenAtGreaterThanEqualAndEatenAtLessThanOrderByEatenAtAsc(
			Long userId, LocalDateTime start, LocalDateTime end);

	Long user(User user);
}
