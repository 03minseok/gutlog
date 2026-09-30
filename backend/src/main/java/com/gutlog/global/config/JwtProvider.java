package com.gutlog.global.config;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtProvider {
    private final SecretKey secretKey;
    private final long expirationsMs;

    public JwtProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration}") long expirationsMs
    ){
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationsMs = expirationsMs;
    }

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
    public boolean validateToken(String token){
        try{
            Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token);
            return true;
        } catch (Exception e){
            return false; // 위조, 만료, 형식 오류
        }
    }

    public Long getUserId(String token){
        return Long.valueOf(
                Jwts.parser().verifyWith(secretKey).build()
                        .parseSignedClaims(token).getPayload().getSubject()
        );
    }
}
