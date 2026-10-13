// [권한] 직접 작성 연습용. '// [권한]' 주석이 Claude가 작성했던 권한 코드이고, 지금 동작하는 코드는 권한 추가 전 상태다.
// [권한] 다시 작성할 때: 토큰에서 꺼낸 권한을 "ROLE_" + 권한 이름으로 만들어 인증 정보에 함께 넣는다.
package com.gutlog.global.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
// [권한] import org.springframework.security.core.authority.SimpleGrantedAuthority;
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
        // [권한] if(token != null){
        // [권한]     jwtProvider.parse(token).ifPresent(info -> {
        // [권한]         // 이 요청은 인증된 사용자로 처리할 수 있음. 권한은 ROLE_USER / ROLE_ADMIN (hasRole("ADMIN")으로 검사)
        // [권한]         // hasRole("ADMIN")은 내부적으로 "ROLE_ADMIN"이라는 이름을 찾기 때문에 "ROLE_"을 붙인다.
        // [권한]         List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + info.authority().name()));
        // [권한]         UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(info.userId(), null, authorities);
        // [권한]         SecurityContextHolder.getContext().setAuthentication(authenticationToken);
        // [권한]     });
        // [권한] }
        if(token != null && jwtProvider.validateToken(token)){
            Long userId = jwtProvider.getUserId(token);

            // 이 요청은 인증된 사용자로 처리할 수 있음
            // [권한] 세 번째 인자 List.of()가 "권한 목록"이다. 지금은 비어 있어서 hasRole 검사를 통과할 수 없다.
            UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(userId, null, List.of());
            SecurityContextHolder.getContext().setAuthentication(authenticationToken);
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
