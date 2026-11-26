package com.gutlog.domain.guardian.service;

import com.gutlog.domain.guardian.dto.response.GuardianPatientResponse;
import com.gutlog.domain.guardian.entity.GuardianCodeAttempt;
import com.gutlog.domain.guardian.entity.GuardianInviteCode;
import com.gutlog.domain.guardian.entity.GuardianLink;
import com.gutlog.domain.guardian.repository.GuardianCodeAttemptRepository;
import com.gutlog.domain.guardian.repository.GuardianInviteCodeRepository;
import com.gutlog.domain.guardian.repository.GuardianLinkRepository;
import com.gutlog.domain.user.entity.Role;
import com.gutlog.domain.user.entity.User;
import com.gutlog.domain.user.repository.UserRepository;
import com.gutlog.global.exception.BusinessException;
import com.gutlog.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * 보호자의 코드 입력을 한 트랜잭션으로 처리한다.
 * 실패 횟수가 롤백되지 않도록 예외를 던지지 않고 ConnectResult만 돌려준다. 예외는 GuardianConnectService가 커밋 뒤에 던진다. (design.md 5.1)
 */
@Component
@RequiredArgsConstructor
public class GuardianConnectProcessor {
	private final UserRepository userRepository;
	private final GuardianInviteCodeRepository inviteCodeRepository;
	private final GuardianLinkRepository linkRepository;
	private final GuardianCodeAttemptRepository attemptRepository;
	private final Clock clock;

	private final int MAX_GUARDIAN_COUNT=2;

	// 코드 입력 처리: 역할 확인 → 보호자 행 잠금 → 차단·개수·코드·환자 연결 확인 → 연결 생성, 코드 삭제, 실패 횟수 초기화
	// (design.md 4.2, 5.1, 5.2, C1, Q6, Q8)
	@Transactional
	public ConnectResult process(Long guardianId, String code) {
		// 보호자 행 잠금, GUARDIAN이 아니면 404
		User guardian = userRepository.findByIdForUpdate(guardianId)
				.orElseThrow(()->new BusinessException(ErrorCode.USER_NOT_FOUND));
		if(!guardian.getRole().equals(Role.GUARDIAN)){
			throw new BusinessException(ErrorCode.GUARDIAN_LINK_NOT_FOUND);
		}

		LocalDateTime now = LocalDateTime.now(clock);

		// 보호자 연결 시도 이력 찾기, 없으면 생성
		GuardianCodeAttempt attempt = attemptRepository.findByGuardianId(guardianId)
				.orElse(GuardianCodeAttempt.create(guardian));
		attemptRepository.save(attempt);

		// 막혀 있으면 코드를 보지 않고 429
		if(attempt.isBlocked(now)){
			return ConnectResult.failure(ErrorCode.INVITE_CODE_BLOCKED);
		}

		// 이미 환자 2명과 연결돼 있으면 코드를 보지 않고 409
		if((long) linkRepository.findAllByGuardianIdOrderByConnectedAtAscIdAsc(guardianId).size() >= MAX_GUARDIAN_COUNT){
			throw new BusinessException(ErrorCode.GUARDIAN_PATIENT_LIMIT);
		}

		// 코드 찾기 -> 없거나 만료면 실패 +1, 이번 실패로 막혔으면 429 아니면 404
		GuardianInviteCode inviteCode = inviteCodeRepository.findByCodeForUpdate(code)
						.filter(ic -> !ic.isExpired(now)).orElse(null);
		if(inviteCode==null){
			attempt.recordFailure(now);
			if(attempt.isBlocked(now)){
				return ConnectResult.failure(ErrorCode.INVITE_CODE_BLOCKED);
			}
			return ConnectResult.failure(ErrorCode.INVITE_CODE_NOT_FOUND);
		}

		// 환자에게 이미 보호자가 있으면 409
		if(linkRepository.findByPatientId(inviteCode.getPatient().getId()).isPresent()){
			throw new BusinessException(ErrorCode.PATIENT_ALREADY_LINKED);
		}

		// 연결 생성(공개 범위 전부 비공개), 실패 횟수 초기화, 코드 삭제
		GuardianLink link = linkRepository.save(GuardianLink.create(inviteCode.getPatient(), guardian, now));
		attempt.reset();
		inviteCodeRepository.delete(inviteCode);
		return ConnectResult.success(GuardianPatientResponse.fromEntity(link));
	}
}
