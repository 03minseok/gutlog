package com.gutlog.domain.record.medication.repository;

import com.gutlog.domain.record.medication.entity.MedicationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface MedicationLogRepository extends JpaRepository<MedicationLog, Long> {

	/** 삭제 표시된 기록도 찾는다. (다시 체크할 때 되돌리기 위해) */
	Optional<MedicationLog> findByUserMedicineIdAndTakenDateAndDoseNumber(Long userMedicineId, LocalDate takenDate, int doseNumber);

	boolean existsByUserMedicineIdAndTakenDateAndDoseNumberAndDeletedFalse(Long userMedicineId, LocalDate takenDate, int doseNumber);

	/** 그날 내 복용 기록 (끊은 약 포함). 복용 약·약 정보까지 한 번에 */
	@Query("""
			select l from MedicationLog l
			join fetch l.userMedicine um
			join fetch um.medicine
			where um.user.id = :userId and l.takenDate = :date and l.deleted = false
			order by um.id asc, l.doseNumber asc
			""")
	List<MedicationLog> findTakenOn(@Param("userId") Long userId, @Param("date") LocalDate date);

	/** 기간 조회: from 이상, end 미만 (end = to + 1일) */
	@Query("""
			select l from MedicationLog l
			join fetch l.userMedicine um
			join fetch um.medicine
			where um.user.id = :userId and l.takenDate >= :from and l.takenDate < :end and l.deleted = false
			order by l.takenDate asc, um.id asc, l.doseNumber asc
			""")
	List<MedicationLog> findTakenBetween(@Param("userId") Long userId, @Param("from") LocalDate from, @Param("end") LocalDate end);

	/** 약별 마지막 투여일 (date 이전, date 포함). INTERVAL 약의 다음 예정일 계산용 */
	@Query("""
			select new com.gutlog.domain.record.medication.repository.LastTakenDate(l.userMedicine.id, max(l.takenDate))
			from MedicationLog l
			where l.userMedicine.id in :userMedicineIds and l.deleted = false and l.takenDate <= :date
			group by l.userMedicine.id
			""")
	List<LastTakenDate> findLastTakenDates(@Param("userMedicineIds") Collection<Long> userMedicineIds, @Param("date") LocalDate date);
}
