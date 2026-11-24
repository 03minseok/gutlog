package com.gutlog.domain.guardian.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 초대 코드 문자열 생성 (design.md 4.1). 겹침 확인은 서비스 몫이라 여기서는 형식만 본다.
 */
class InviteCodeGeneratorTest {

	private final InviteCodeGenerator generator = new InviteCodeGenerator();

	@Test
	@DisplayName("1,000번 만든 코드가 모두 영문 대문자+숫자 6자리다")
	void format() {
		for (int i = 0; i < 1_000; i++) {
			assertThat(generator.generate()).matches("^[A-Z0-9]{6}$");
		}
	}

	@Test
	@DisplayName("매번 같은 코드를 내지 않는다")
	void notConstant() {
		Set<String> codes = new HashSet<>();
		for (int i = 0; i < 100; i++) {
			codes.add(generator.generate());
		}
		assertThat(codes.size()).isGreaterThan(90);
	}
}
