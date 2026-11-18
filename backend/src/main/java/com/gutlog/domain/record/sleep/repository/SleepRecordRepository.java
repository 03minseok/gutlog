package com.gutlog.domain.record.sleep.repository;

import com.gutlog.domain.record.sleep.entity.SleepRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SleepRecordRepository extends JpaRepository<SleepRecord, Long> {
	Optional<SleepRecord> findByUserIdAndRecordDate(Long userId, LocalDate recordDate);

	List<SleepRecord> findAllByUserIdAndRecordDateBetweenOrderByRecordDateAsc(Long userId, LocalDate from, LocalDate to);
}
