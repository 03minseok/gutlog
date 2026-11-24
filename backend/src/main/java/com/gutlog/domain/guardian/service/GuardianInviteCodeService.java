package com.gutlog.domain.guardian.service;

import com.gutlog.domain.guardian.dto.response.InviteCodeResponse;
import com.gutlog.domain.guardian.entity.GuardianInviteCode;
import com.gutlog.domain.guardian.repository.GuardianInviteCodeRepository;
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

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GuardianInviteCodeService {
	private final GuardianInviteCodeRepository inviteCodeRepository;
	private final GuardianLinkRepository linkRepository;
	private final UserRepository userRepository;
	private final InviteCodeGenerator inviteCodeGenerator;
	private final Clock clock;
	private static final int VALID_MINUTE = 10;

	// 현재 코드 조회: PATIENT가 아니면 404, 코드가 없거나 만료됐으면 null을 돌려준다 (design.md 3.1, 4.1)
	public InviteCodeResponse getInviteCode(Long userId) {
		checkPatient(userId);
		return inviteCodeRepository.findByPatientId(userId)
				.filter(code->!code.isExpired(LocalDateTime.now(clock)))
				.map(InviteCodeResponse::fromEntity)
				.orElse(null);
	}

	// 발급·재발급: PATIENT가 아니면 404, 보호자와 연결돼 있으면 409,
	// 아니면 어떤 행과도 겹치지 않는 코드를 Clock 기준 10분 뒤 만료로 저장한다 (환자 행이 있으면 교체) (design.md 3.1, 4.1, Q7)
	@Transactional
	public InviteCodeResponse issueInviteCode(Long userId) {
		// patient아닌지, 보호자 연결되어 있는지 확인
		User user = checkPatient(userId);
		if(linkRepository.findByPatientId(userId).isPresent()){
			throw new BusinessException(ErrorCode.GUARDIAN_ALREADY_LINKED);
		}
		// 발급, 재발급
		String inviteCode;
		do{
			inviteCode = inviteCodeGenerator.generate();
		}while (inviteCodeRepository.existsByCode(inviteCode));
		LocalDateTime expiredAt = LocalDateTime.now(clock).plusMinutes(VALID_MINUTE);

		String finalInviteCode = inviteCode;
		GuardianInviteCode code = inviteCodeRepository.findByPatientId(userId).
				map(c->{
					c.reissue(finalInviteCode, expiredAt);
					return c;
				}).orElseGet(()->GuardianInviteCode.create(user, finalInviteCode, expiredAt));
		inviteCodeRepository.save(code);
		return InviteCodeResponse.fromEntity(code);
	}

	private User checkPatient(Long userId){
		User user = userRepository.findById(userId).orElseThrow(()-> new BusinessException(ErrorCode.USER_NOT_FOUND));
		if(user.getRole()!= Role.PATIENT){
			throw new BusinessException(ErrorCode.GUARDIAN_LINK_NOT_FOUND);
		}
		return user;
	}
}
