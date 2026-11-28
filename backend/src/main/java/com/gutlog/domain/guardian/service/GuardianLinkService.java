package com.gutlog.domain.guardian.service;

import com.gutlog.domain.guardian.dto.response.GuardianPatientResponse;
import com.gutlog.domain.guardian.dto.response.MyGuardianResponse;
import com.gutlog.domain.guardian.entity.GuardianLink;
import com.gutlog.domain.guardian.entity.GuardianLinkHistory;
import com.gutlog.domain.guardian.repository.GuardianLinkHistoryRepository;
import com.gutlog.domain.guardian.repository.GuardianLinkRepository;
import com.gutlog.domain.user.entity.Role;
import com.gutlog.domain.user.entity.User;
import com.gutlog.domain.user.repository.UserRepository;
import com.gutlog.global.exception.BusinessException;
import com.gutlog.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GuardianLinkService {
	private final GuardianLinkRepository linkRepository;
	private final GuardianLinkHistoryRepository historyRepository;
	private final UserRepository userRepository;
	private final Clock clock;

	// design.md 3.1, C5
	public MyGuardianResponse getMyGuardian(Long userId) {
		User patient = userRepository.findById(userId)
				.orElseThrow(()-> new BusinessException(ErrorCode.USER_NOT_FOUND));
		if(!patient.getRole().equals(Role.PATIENT)){
			throw new BusinessException(ErrorCode.GUARDIAN_LINK_NOT_FOUND);
		}
		return linkRepository.findByPatientId(userId)
				.map(MyGuardianResponse::fromEntity)
				.orElse(null);
	}

	// design.md 2장(guardian_link_history), 3.1, Q2, Q3
	@Transactional
	public void disconnectMyGuardian(Long userId) {
		User patient = userRepository.findById(userId)
				.orElseThrow(()-> new BusinessException(ErrorCode.USER_NOT_FOUND));
		if(!patient.getRole().equals(Role.PATIENT)){
			throw new BusinessException(ErrorCode.GUARDIAN_LINK_NOT_FOUND);
		}
		GuardianLink link = linkRepository.findByPatientIdForUpdate(userId)
				.orElseThrow(()->new BusinessException(ErrorCode.GUARDIAN_LINK_NOT_FOUND));
		GuardianLinkHistory history = GuardianLinkHistory.of(link, LocalDateTime.now(clock), patient);
		historyRepository.save(history);
		linkRepository.delete(link);
	}

	// design.md 3.2, Q15
	public List<GuardianPatientResponse> getPatients(Long userId) {
		User guardian = userRepository.findById(userId)
				.orElseThrow(()-> new BusinessException(ErrorCode.USER_NOT_FOUND));
		if(!guardian.getRole().equals(Role.GUARDIAN)){
			throw new BusinessException(ErrorCode.GUARDIAN_LINK_NOT_FOUND);
		}
        return linkRepository.findAllByGuardianIdOrderByConnectedAtAscIdAsc(userId)
                .stream().map(GuardianPatientResponse::fromEntity).toList();
	}

	// design.md 2장(guardian_link_history), 3.2, Q2, Q3, Q9
	@Transactional
	public void disconnectPatient(Long userId, Long patientId) {
		User guardian = userRepository.findById(userId) //본인
				.orElseThrow(()->new BusinessException(ErrorCode.USER_NOT_FOUND));
		if(!guardian.getRole().equals(Role.GUARDIAN)){
			throw new BusinessException(ErrorCode.GUARDIAN_LINK_NOT_FOUND);
		}
		User patient = userRepository.findById(patientId)
				.orElseThrow(()->new BusinessException(ErrorCode.GUARDIAN_LINK_NOT_FOUND));
		if(!patient.getRole().equals(Role.PATIENT)){
			throw new BusinessException(ErrorCode.GUARDIAN_LINK_NOT_FOUND);
		}
		GuardianLink guardianLink = linkRepository.findByPatientIdAndGuardianIdForUpdate(patientId, userId)
				.orElseThrow(()-> new BusinessException(ErrorCode.GUARDIAN_LINK_NOT_FOUND));

		GuardianLinkHistory history = GuardianLinkHistory.of(guardianLink, LocalDateTime.now(clock), guardian);
		historyRepository.save(history);
		linkRepository.delete(guardianLink);
	}
}
