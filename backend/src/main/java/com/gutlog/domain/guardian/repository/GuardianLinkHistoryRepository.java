package com.gutlog.domain.guardian.repository;

import com.gutlog.domain.guardian.entity.GuardianLinkHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GuardianLinkHistoryRepository extends JpaRepository<GuardianLinkHistory, Long> {
}
