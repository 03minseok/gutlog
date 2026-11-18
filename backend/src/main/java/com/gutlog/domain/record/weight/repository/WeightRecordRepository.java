package com.gutlog.domain.record.weight.repository;

import com.gutlog.domain.record.weight.entity.WeightRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface WeightRecordRepository extends JpaRepository<WeightRecord, Long> {
	Optional<WeightRecord> findByUserIdAndRecordDate(Long userId, LocalDate recordDate);

	List<WeightRecord> findAllByUserIdAndRecordDateBetweenOrderByRecordDateAsc(Long userId, LocalDate from, LocalDate to);
}
