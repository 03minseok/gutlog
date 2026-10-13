package com.gutlog.domain.record.medication.repository;

import com.gutlog.domain.record.medication.entity.UserMedicine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface UserMedicineRepository extends JpaRepository<UserMedicine, Long> {

	/** 내 복용 약 목록 (끊지 않은 약). 약 정보까지 한 번에 */
	@Query("""
			select um from UserMedicine um
			join fetch um.medicine
			where um.user.id = :userId and um.deleted = false
			order by um.id asc
			""")
	List<UserMedicine> findActiveWithMedicine(@Param("userId") Long userId);

	/**
	 * 그날 복용 중이던 약: 그날이 끝나기 전에 등록했고, 아직 안 끊었거나 그날이 끝난 뒤에 끊었다.
	 * dayEnd = 조회 날짜 + 1일 00:00
	 */
	@Query("""
			select um from UserMedicine um
			join fetch um.medicine
			where um.user.id = :userId
			and um.createdAt < :dayEnd
			and (um.deleted = false or um.deletedAt >= :dayEnd)
			order by um.id asc
			""")
	List<UserMedicine> findInUseOn(@Param("userId") Long userId, @Param("dayEnd") LocalDateTime dayEnd);

	boolean existsByUserIdAndMedicineIdAndDeletedFalse(Long userId, Long medicineId);
}
