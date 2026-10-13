package com.gutlog.global.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException{
        String token = resolveToken(request);
        if(token != null){
            jwtProvider.parse(token).ifPresent(info -> {
                // 이 요청은 인증된 사용자로 처리할 수 있음. 권한은 ROLE_USER / ROLE_ADMIN (hasRole("ADMIN")으로 검사)
                List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + info.authority().name()));
                UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(info.userId(), null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            });
        }
        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request){
        String bearer = request.getHeader("Authorization");
        if(bearer != null && bearer.startsWith("Bearer ")){
            return bearer.substring(7);
        }
        return null;
    }
}
