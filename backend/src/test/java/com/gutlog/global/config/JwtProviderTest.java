// [권한] 직접 작성 연습용. '// [권한]' 주석은 권한 기능용 테스트다. 권한 코드를 다시 작성한 뒤 주석을 풀어 확인한다.
package com.gutlog.global.config;

// [권한] import com.gutlog.domain.user.entity.Authority;
// [권한] import io.jsonwebtoken.Jwts;
// [권한] import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

// [권한] import java.nio.charset.StandardCharsets;

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

// [권한]     @Test
// [권한]     @DisplayName("권한을 토큰에 담고 parse로 userId와 함께 꺼낸다")
// [권한]     void authorityClaim() {
// [권한]         String admin = jwtProvider.createToken(7L, Authority.ADMIN);
// [권한]         String user = jwtProvider.createToken(8L, Authority.USER);
// [권한]
// [권한]         assertThat(jwtProvider.parse(admin)).hasValue(new JwtProvider.TokenInfo(7L, Authority.ADMIN));
// [권한]         assertThat(jwtProvider.parse(user)).hasValue(new JwtProvider.TokenInfo(8L, Authority.USER));
// [권한]     }
// [권한]
// [권한]     @Test
// [권한]     @DisplayName("권한 claim이 없는 예전 토큰은 USER로 본다")
// [권한]     void legacyTokenWithoutAuthority() {
// [권한]         String legacy = Jwts.builder()
// [권한]                 .subject("9")
// [권한]                 .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
// [권한]                 .compact();
// [권한]
// [권한]         assertThat(jwtProvider.parse(legacy)).hasValue(new JwtProvider.TokenInfo(9L, Authority.USER));
// [권한]     }
// [권한]
// [권한]     @Test
// [권한]     @DisplayName("위조·형식 오류 토큰은 parse 결과가 비어 있다")
// [권한]     void parseInvalid() {
// [권한]         String forged = new JwtProvider(OTHER_SECRET, 3_600_000L).createToken(1L, Authority.ADMIN);
// [권한]
// [권한]         assertThat(jwtProvider.parse(forged)).isEmpty();
// [권한]         assertThat(jwtProvider.parse("not-a-jwt")).isEmpty();
// [권한]     }
}
