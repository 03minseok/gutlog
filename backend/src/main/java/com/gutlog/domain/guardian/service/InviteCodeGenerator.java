package com.gutlog.domain.guardian.service;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * 초대 코드 문자열을 만든다. 다른 행과 겹치는지는 서비스에서 확인한다.
 */
@Component
public class InviteCodeGenerator {

	private static final String CHAR_POOL = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
	private static final int CODE_LENGTH = 6;
	private static final SecureRandom RANDOM = new SecureRandom();
	// design.md 4.1
	public String generate() {
		StringBuilder sb = new StringBuilder(CODE_LENGTH);
		for(int i=0; i<CODE_LENGTH; i++){
			int index = RANDOM.nextInt(CHAR_POOL.length());
			sb.append(CHAR_POOL.charAt(index));
		}
		return sb.toString();
	}
}
