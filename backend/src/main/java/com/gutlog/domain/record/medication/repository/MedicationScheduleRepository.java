package com.gutlog.domain.record.medication.repository;

import com.gutlog.domain.record.medication.entity.MedicationSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MedicationScheduleRepository extends JpaRepository<MedicationSchedule, Long> {
    // 약 하나의 현재 일정 (수정, 삭제할 때 닫을 대상)
    @Query("""
        select s from MedicationSchedule s 
        where s.userMedicine.id = :userMedicineId and s.effectiveTo is null
        """)
    Optional<MedicationSchedule> findCurrent(Long userMedicineId);

    // 약 하나의 그날 일정 (체크할 때 회차 상한 검사)
    @Query("""
        select s from MedicationSchedule s 
        where s.userMedicine.id = :userMedicineId 
          and s.effectiveFrom <= :date and (s.effectiveTo is null or s.effectiveTo > :date)
        """)
    Optional<MedicationSchedule> findInEffectOn(Long userMedicineId, LocalDate date);

    // 사용자의 모든 약의 그 날짜 일정 (날짜별 조회 화면과 분모 계산)
    @Query("""
    select  s from MedicationSchedule s
        join fetch s.userMedicine um
        join fetch um.medicine
    where um.user.id = :userId
        and s.effectiveFrom <= :date
        and (s.effectiveTo is null or s.effectiveTo > :date)
    """)
    List<MedicationSchedule> findAllInEffectOn(Long userId, LocalDate date);

    //끊지 않은 약물의 현재 일정 (내 복용 약 목록)
    @Query("""
        select s from MedicationSchedule s
        join fetch s.userMedicine um
        join fetch um.medicine
        where um.user.id = :userId and um.deleted = false and s.effectiveTo is null
        order by um.id
        """)
    List<MedicationSchedule> findCurrentByUserId(Long userId);

    //그 날짜 이전에 시작한 마지막 일정 (그날 끊었지만 기록이 있는 약을 보여줄 때)
    @Query("""
        select s from MedicationSchedule s
        join fetch s.userMedicine um
        join fetch um.medicine
        where um.id in :userMedicineIds
          and s.effectiveFrom = (
              select max(s2.effectiveFrom) from MedicationSchedule s2
              where s2.userMedicine = um and s2.effectiveFrom <= :date)
        """)
    List<MedicationSchedule> findLatestStartedBy(List<Long> userMedicineIds, LocalDate date);
}
