package com.gutlog.global.config;

import com.gutlog.domain.user.entity.Authority;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 실제 포트로 띄워서 보는 401/403 상태 코드.
 * MockMvc는 sendError 뒤의 /error 디스패치를 하지 않아서, 403이 401로 바뀌는 문제를 잡지 못한다.
 * 그래서 이 테스트만 진짜 HTTP 요청을 보낸다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SecurityHttpStatusTest {

    @LocalServerPort int port;
    @Autowired JwtProvider jwtProvider;

    private final HttpClient client = HttpClient.newHttpClient();

    private int statusOf(String path, String token) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET();
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        return client.send(request.build(), HttpResponse.BodyHandlers.discarding()).statusCode();
    }

    @Test
    @DisplayName("일반 사용자가 관리자 API에 접근하면 실제 서버에서도 403 (401로 바뀌지 않는다)")
    void userGetsForbidden() throws Exception {
        assertThat(statusOf("/api/admin/medicines", jwtProvider.createToken(1L, Authority.USER))).isEqualTo(403);
    }

    @Test
    @DisplayName("토큰이 없으면 401")
    void anonymousGetsUnauthorized() throws Exception {
        assertThat(statusOf("/api/admin/medicines", null)).isEqualTo(401);
        assertThat(statusOf("/api/my-medicines", null)).isEqualTo(401);
    }

    @Test
    @DisplayName("ADMIN 토큰이면 통과")
    void adminAllowed() throws Exception {
        assertThat(statusOf("/api/admin/medicines", jwtProvider.createToken(1L, Authority.ADMIN))).isEqualTo(200);
    }
}
