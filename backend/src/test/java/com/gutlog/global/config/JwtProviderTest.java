package com.gutlog.global.config;

import com.gutlog.domain.user.entity.Authority;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

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
        String token = jwtProvider.createToken(42L, Authority.USER);

        assertThat(jwtProvider.validateToken(token)).isTrue();
        assertThat(jwtProvider.getUserId(token)).isEqualTo(42L);
    }

    @Test
    @DisplayName("만료된 토큰은 검증 실패")
    void expired() {
        JwtProvider expiredProvider = new JwtProvider(SECRET, -1_000L); // 발급 즉시 만료
        String token = expiredProvider.createToken(1L, Authority.USER);

        assertThat(jwtProvider.validateToken(token)).isFalse();
    }

    @Test
    @DisplayName("다른 키로 서명된(위조된) 토큰은 검증 실패")
    void wrongSignature() {
        String forged = new JwtProvider(OTHER_SECRET, 3_600_000L).createToken(1L, Authority.USER);

        assertThat(jwtProvider.validateToken(forged)).isFalse();
    }

    @Test
    @DisplayName("형식이 깨진 토큰은 검증 실패")
    void malformed() {
        assertThat(jwtProvider.validateToken("not-a-jwt")).isFalse();
        assertThat(jwtProvider.validateToken("")).isFalse();
    }

    @Test
    @DisplayName("권한을 토큰에 담고 parse로 userId와 함께 꺼낸다")
    void authorityClaim() {
        String admin = jwtProvider.createToken(7L, Authority.ADMIN);
        String user = jwtProvider.createToken(8L, Authority.USER);

        assertThat(jwtProvider.parse(admin)).hasValue(new JwtProvider.TokenInfo(7L, Authority.ADMIN));
        assertThat(jwtProvider.parse(user)).hasValue(new JwtProvider.TokenInfo(8L, Authority.USER));
    }

    @Test
    @DisplayName("권한 claim이 없는 예전 토큰은 USER로 본다")
    void legacyTokenWithoutAuthority() {
        String legacy = Jwts.builder()
                .subject("9")
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();

        assertThat(jwtProvider.parse(legacy)).hasValue(new JwtProvider.TokenInfo(9L, Authority.USER));
    }

    @Test
    @DisplayName("위조·형식 오류 토큰은 parse 결과가 비어 있다")
    void parseInvalid() {
        String forged = new JwtProvider(OTHER_SECRET, 3_600_000L).createToken(1L, Authority.ADMIN);

        assertThat(jwtProvider.parse(forged)).isEmpty();
        assertThat(jwtProvider.parse("not-a-jwt")).isEmpty();
    }
}
