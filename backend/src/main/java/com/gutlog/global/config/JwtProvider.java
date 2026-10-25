package com.gutlog.global.config;

import com.gutlog.domain.user.entity.Authority;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;

@Component
public class JwtProvider {
    private static final String AUTHORITY_CLAIM = "auth";

    private final SecretKey secretKey;
    private final long expirationsMs;

    public JwtProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration}") long expirationsMs
    ){
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationsMs = expirationsMs;
    }

    // 권한을 토큰에 넣는다. 권한이 바뀌면 다시 로그인(또는 토큰 만료)해야 반영된다.
    public String createToken(Long userId, Authority authority){
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationsMs);

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim(AUTHORITY_CLAIM, authority.name())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey)
                .compact();
    }
    // 토큰 검증 -> verifyWith(secretKey)로 서명 검증, parseSignedClaims(token)로 토큰 파싱
    public boolean validateToken(String token){
        return parse(token).isPresent();
    }

    public Long getUserId(String token){
        return Long.valueOf(parseClaims(token).getSubject());
    }

    /**
     * 검증과 파싱을 한 번에 한다. 위조·만료·형식 오류면 비어 있다.
     * 권한 claim이 없거나 알 수 없는 값이면 USER로 본다. (권한 claim이 생기기 전에 발급된 토큰)
     */
    public Optional<TokenInfo> parse(String token){
        try{
            Claims claims = parseClaims(token);
            return Optional.of(new TokenInfo(Long.valueOf(claims.getSubject()), authorityOf(claims)));
        } catch (Exception e){
            return Optional.empty(); // 위조, 만료, 형식 오류
        }
    }

    private Claims parseClaims(String token){
        return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload();
    }

    private static Authority authorityOf(Claims claims){
        String value = claims.get(AUTHORITY_CLAIM, String.class);
        try{
            return value == null ? Authority.USER : Authority.valueOf(value);
        } catch (IllegalArgumentException e){
            return Authority.USER;
        }
    }

    public record TokenInfo(Long userId, Authority authority) {}
}
