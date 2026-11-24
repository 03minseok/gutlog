package com.gutlog.domain.guardian.repository;

import com.gutlog.domain.guardian.entity.GuardianCodeAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GuardianCodeAttemptRepository extends JpaRepository<GuardianCodeAttempt, Long> {
	Optional<GuardianCodeAttempt> findByGuardianId(Long guardianId);
}
