// [권한] 직접 작성 연습용. '// [권한]' 주석이 Claude가 작성했던 권한 코드이고, 지금 동작하는 코드는 권한 추가 전 상태다.
// [권한] 다시 작성할 때: /api/admin/** 에 hasRole("ADMIN")을 걸고, 에러 디스패치를 통과시켜 403이 401로 바뀌지 않게 한다.
package com.gutlog.global.config;

// [권한] import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception{
        http
                .csrf(csrf -> csrf.disable())
                .headers(h -> h.frameOptions(f -> f.disable()))
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 규칙은 위에서부터 순서대로 검사한다. 먼저 맞는 규칙이 적용된다.
                        // [권한] sendError(401/403)는 /error로 다시 디스패치된다. 이때는 인증 정보가 없어서
                        // [권한] /error까지 막으면 403이 401로 바뀐다. 에러 디스패치는 통과시켜 원래 상태 코드를 그대로 보낸다.
                        // [권한] .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers("/api/users/signup", "/api/users/login", "/api/users/check-username",
                            "/h2-console/**",
                            "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                        // [권한] 관리자 API: ADMIN 권한만. 일반 사용자는 403
                        // [권한] .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        // 임시: 권한을 다시 작성하기 전까지 관리자 API를 아무도 못 쓰게 잠근다.
                        // (이 줄이 없으면 로그인만 하면 누구나 약 마스터를 추가·수정할 수 있다) 위 hasRole 줄로 바꾸면 된다.
                        .requestMatchers("/api/admin/**").denyAll()
                        .anyRequest().authenticated())
                .exceptionHandling(e -> e
                        // 401: 토큰이 없거나 만료/위조 됨
                        .authenticationEntryPoint((request, response, ex) ->
                        response.sendError(HttpServletResponse.SC_UNAUTHORIZED))
                        // 403: 인증은 됐지만 권한이 없음
                        .accessDeniedHandler((request, response, ex) ->
                                response.sendError(HttpServletResponse.SC_FORBIDDEN)))

                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }
}
