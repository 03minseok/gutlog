package com.gutlog.domain.guardian.repository;

import com.gutlog.domain.guardian.entity.GuardianLink;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GuardianLinkRepository extends JpaRepository<GuardianLink, Long> {
	/** 환자의 현재 연결. 환자당 최대 1개다. */
	Optional<GuardianLink> findByPatientId(Long patientId);

	/** 보호자가 이 환자와 연결되어 있는지 확인할 때 쓴다. */
	Optional<GuardianLink> findByPatientIdAndGuardianId(Long patientId, Long guardianId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select l from GuardianLink l where l.patient.id = :patientId and l.guardian.id = :guardianId")
	Optional<GuardianLink> findByPatientIdAndGuardianIdForUpdate(@Param("patientId") Long patientId, @Param("guardianId") Long guardianId);

	/** 보호자의 연결 목록. 먼저 연결한 순이다. */
	List<GuardianLink> findAllByGuardianIdOrderByConnectedAtAscIdAsc(Long guardianId);

	long countByGuardianId(Long guardianId);

	/**
	 * 환자의 현재 연결을 SELECT ... FOR UPDATE로 잠그고 찾는다.
	 * 환자와 보호자가 같은 연결을 동시에 해제해도 한 명만 해제하고, 늦은 요청은 연결이 없어 404가 된다. (G05)
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select l from GuardianLink l where l.patient.id = :patientId")
	Optional<GuardianLink> findByPatientIdForUpdate(@Param("patientId") Long patientId);
}
