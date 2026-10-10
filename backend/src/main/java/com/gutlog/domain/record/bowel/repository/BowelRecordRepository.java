package com.gutlog.domain.record.bowel.repository;

import com.gutlog.domain.record.bowel.entity.BowelRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BowelRecordRepository extends JpaRepository<BowelRecord, Long> {
	List<BowelRecord> findAllByUserIdAndBowelAtGreaterThanEqualAndBowelAtLessThanOrderByBowelAtAsc(Long userId, LocalDateTime start, LocalDateTime end);
}
