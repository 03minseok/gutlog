package com.gutlog.domain.guardian.service;

import com.gutlog.domain.guardian.dto.response.InviteCodeResponse;
import com.gutlog.domain.guardian.repository.GuardianInviteCodeRepository;
import com.gutlog.domain.guardian.repository.GuardianLinkRepository;
import com.gutlog.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GuardianInviteCodeService {
	private final GuardianInviteCodeRepository inviteCodeRepository;
	private final GuardianLinkRepository linkRepository;
	private final UserRepository userRepository;
	private final InviteCodeGenerator inviteCodeGenerator;
	private final Clock clock;

	// design.md 3.1
	public InviteCodeResponse getInviteCode(Long userId) {
		throw new UnsupportedOperationException("TODO: 개발자 구현");
	}

	// design.md 3.1, 4.1, Q7
	@Transactional
	public InviteCodeResponse issueInviteCode(Long userId) {
		throw new UnsupportedOperationException("TODO: 개발자 구현");
	}
}
