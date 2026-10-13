// [권한] 직접 작성 연습용. '// [권한]' 주석이 Claude가 작성했던 권한 코드이고, 지금 동작하는 코드는 권한 추가 전 상태다.
// [권한] 다시 작성할 때: createToken에 권한 claim을 넣고, 검증·파싱을 한 번에 하는 parse()를 만들어 JwtFilter에서 쓴다.
package com.gutlog.global.config;

// [권한] import com.gutlog.domain.user.entity.Authority;
// [권한] import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
// [권한] import java.util.Optional;

@Component
public class JwtProvider {
    // [권한] private static final String AUTHORITY_CLAIM = "auth";   // 토큰 안에서 권한을 담는 칸의 이름

    private final SecretKey secretKey;
    private final long expirationsMs;

    public JwtProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration}") long expirationsMs
    ){
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationsMs = expirationsMs;
    }

    // [권한] 권한을 토큰에 넣는다. 권한이 바뀌면 다시 로그인(또는 토큰 만료)해야 반영된다.
    // [권한] public String createToken(Long userId, Authority authority){
    // [권한]     Date now = new Date();
    // [권한]     Date expiry = new Date(now.getTime() + expirationsMs);
    // [권한]
    // [권한]     return Jwts.builder()
    // [권한]             .subject(String.valueOf(userId))
    // [권한]             .claim(AUTHORITY_CLAIM, authority.name())   // ← 추가된 한 줄: "auth": "ADMIN"
    // [권한]             .issuedAt(now)
    // [권한]             .expiration(expiry)
    // [권한]             .signWith(secretKey)
    // [권한]             .compact();
    // [권한] }
    public String createToken(Long userId){
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationsMs);

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey)
                .compact();
    }
    // 토큰 검증 -> verifyWith(secretKey)로 서명 검증, parseSignedClaims(token)로 토큰 파싱
    // [권한] 권한 버전에서는 parse()를 재사용해서 한 줄이 된다:
    // [권한] public boolean validateToken(String token){
    // [권한]     return parse(token).isPresent();
    // [권한] }
    public boolean validateToken(String token){
        try{
            Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token);
            return true;
        } catch (Exception e){
            return false; // 위조, 만료, 형식 오류
        }
    }

    // [권한] public Long getUserId(String token){
    // [권한]     return Long.valueOf(parseClaims(token).getSubject());
    // [권한] }
    public Long getUserId(String token){
        return Long.valueOf(
                Jwts.parser().verifyWith(secretKey).build()
                        .parseSignedClaims(token).getPayload().getSubject()
        );
    }

    // [권한] /**
    // [권한]  * 검증과 파싱을 한 번에 한다. 위조·만료·형식 오류면 비어 있다.
    // [권한]  * (기존에는 validateToken, getUserId가 토큰을 각각 한 번씩, 총 두 번 해석했다)
    // [권한]  * 권한 claim이 없거나 알 수 없는 값이면 USER로 본다. (권한 claim이 생기기 전에 발급된 토큰)
    // [권한]  */
    // [권한] public Optional<TokenInfo> parse(String token){
    // [권한]     try{
    // [권한]         Claims claims = parseClaims(token);
    // [권한]         return Optional.of(new TokenInfo(Long.valueOf(claims.getSubject()), authorityOf(claims)));
    // [권한]     } catch (Exception e){
    // [권한]         return Optional.empty(); // 위조, 만료, 형식 오류
    // [권한]     }
    // [권한] }
    // [권한]
    // [권한] private Claims parseClaims(String token){
    // [권한]     return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload();
    // [권한] }
    // [권한]
    // [권한] private static Authority authorityOf(Claims claims){
    // [권한]     String value = claims.get(AUTHORITY_CLAIM, String.class);
    // [권한]     try{
    // [권한]         return value == null ? Authority.USER : Authority.valueOf(value);
    // [권한]     } catch (IllegalArgumentException e){
    // [권한]         return Authority.USER;
    // [권한]     }
    // [권한] }
    // [권한]
    // [권한] /** parse() 결과: 토큰에서 꺼낸 회원 id와 권한 */
    // [권한] public record TokenInfo(Long userId, Authority authority) {}
}
