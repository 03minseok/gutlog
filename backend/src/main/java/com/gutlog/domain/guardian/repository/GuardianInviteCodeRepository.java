package com.gutlog.domain.guardian.repository;

import com.gutlog.domain.guardian.entity.GuardianInviteCode;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

	/**
	 * 보호자가 입력한 코드를 SELECT ... FOR UPDATE로 잠그고 찾는다. 만료 여부는 호출하는 쪽에서 확인한다.
	 * 같은 코드를 여러 보호자가 동시에 입력해도 한 명만 쓰게 한다. (design.md 4.2의 5단계)
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select c from GuardianInviteCode c where c.code = :code")
	Optional<GuardianInviteCode> findByCodeForUpdate(@Param("code") String code);
}
