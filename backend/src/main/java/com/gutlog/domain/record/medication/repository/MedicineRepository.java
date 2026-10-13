package com.gutlog.domain.record.medication.repository;

import com.gutlog.domain.record.medication.entity.DosageForm;
import com.gutlog.domain.record.medication.entity.Medicine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MedicineRepository extends JpaRepository<Medicine, Long> {
	String ORDER = " order by m.genericName asc, m.brandName asc, m.dosageForm asc";

	/** 활성 약 중 성분명·제품명에 keyword가 들어간 약 */
	@Query("""
			select m from Medicine m
			where m.active = true
			and (m.genericName like concat('%', :keyword, '%') or m.brandName like concat('%', :keyword, '%'))
			""" + ORDER)
	List<Medicine> searchActive(@Param("keyword") String keyword);

	@Query("select m from Medicine m where m.active = true" + ORDER)
	List<Medicine> findAllActive();

	/** 관리자용: 비활성 포함 */
	@Query("select m from Medicine m" + ORDER)
	List<Medicine> findAllOrdered();

	Optional<Medicine> findByGenericNameAndBrandNameAndDosageForm(String genericName, String brandName, DosageForm dosageForm);

	Optional<Medicine> findByIdAndActiveTrue(Long id);
}
