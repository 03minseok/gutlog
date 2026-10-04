package com.gutlog.global.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JwtProvider 단위 테스트. 스프링 없이 생성자로 직접 만든다.
 */
class JwtProviderTest {

    private static final String SECRET = "gutlog-test-secret-key-for-unit-testing-only-must-be-32-bytes-or-more";
    private static final String OTHER_SECRET = "another-secret-key-that-is-also-long-enough-for-hs256-signing";

    private final JwtProvider jwtProvider = new JwtProvider(SECRET, 3_600_000L);

    @Test
    @DisplayName("발급한 토큰은 검증을 통과하고 userId를 꺼낼 수 있다")
    void createAndParse() {
        String token = jwtProvider.createToken(42L);

        assertThat(jwtProvider.validateToken(token)).isTrue();
        assertThat(jwtProvider.getUserId(token)).isEqualTo(42L);
    }

    @Test
    @DisplayName("만료된 토큰은 검증 실패")
    void expired() {
        JwtProvider expiredProvider = new JwtProvider(SECRET, -1_000L); // 발급 즉시 만료
        String token = expiredProvider.createToken(1L);

        assertThat(jwtProvider.validateToken(token)).isFalse();
    }

    @Test
    @DisplayName("다른 키로 서명된(위조된) 토큰은 검증 실패")
    void wrongSignature() {
        String forged = new JwtProvider(OTHER_SECRET, 3_600_000L).createToken(1L);

        assertThat(jwtProvider.validateToken(forged)).isFalse();
    }

    @Test
    @DisplayName("형식이 깨진 토큰은 검증 실패")
    void malformed() {
        assertThat(jwtProvider.validateToken("not-a-jwt")).isFalse();
        assertThat(jwtProvider.validateToken("")).isFalse();
    }
}
