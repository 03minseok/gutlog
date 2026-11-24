package com.gutlog.domain.guardian.repository;

import com.gutlog.domain.guardian.entity.GuardianLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GuardianLinkRepository extends JpaRepository<GuardianLink, Long> {
	/** 환자의 현재 연결. 환자당 최대 1개다. */
	Optional<GuardianLink> findByPatientId(Long patientId);

	/** 보호자가 이 환자와 연결되어 있는지 확인할 때 쓴다. */
	Optional<GuardianLink> findByPatientIdAndGuardianId(Long patientId, Long guardianId);

	/** 보호자의 연결 목록. 먼저 연결한 순이다. */
	List<GuardianLink> findAllByGuardianIdOrderByConnectedAtAscIdAsc(Long guardianId);

	long countByGuardianId(Long guardianId);
}
