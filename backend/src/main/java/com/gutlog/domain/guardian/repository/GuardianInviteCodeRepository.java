package com.gutlog.domain.guardian.repository;

import com.gutlog.domain.guardian.entity.GuardianInviteCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GuardianInviteCodeRepository extends JpaRepository<GuardianInviteCode, Long> {
	/** 환자의 현재 코드. 만료 여부는 호출하는 쪽에서 확인한다. */
	Optional<GuardianInviteCode> findByPatientId(Long patientId);

	/** 보호자가 입력한 코드로 찾는다. 만료 여부는 호출하는 쪽에서 확인한다. */
	Optional<GuardianInviteCode> findByCode(String code);

	/** 새 코드를 만들 때 다른 행과 겹치는지 확인한다. */
	boolean existsByCode(String code);
}
