// [권한] 직접 작성 연습용. '// [권한]' 주석은 권한 기능용 테스트다. 권한 코드를 다시 작성한 뒤 주석을 풀어 확인한다.
// [권한] 실제 포트로 띄워 403이 401로 바뀌지 않는지 확인하는 테스트. SecurityConfig의 에러 디스패치 줄과 짝이다.
// [권한] package com.gutlog.global.config;
// [권한]
// [권한] import com.gutlog.domain.user.entity.Authority;
// [권한] import org.junit.jupiter.api.DisplayName;
// [권한] import org.junit.jupiter.api.Test;
// [권한] import org.springframework.beans.factory.annotation.Autowired;
// [권한] import org.springframework.boot.test.context.SpringBootTest;
// [권한] import org.springframework.boot.test.web.server.LocalServerPort;
// [권한]
// [권한] import java.net.URI;
// [권한] import java.net.http.HttpClient;
// [권한] import java.net.http.HttpRequest;
// [권한] import java.net.http.HttpResponse;
// [권한]
// [권한] import static org.assertj.core.api.Assertions.assertThat;
// [권한]
// [권한] /**
// [권한]  * 실제 포트로 띄워서 보는 401/403 상태 코드.
// [권한]  * MockMvc는 sendError 뒤의 /error 디스패치를 하지 않아서, 403이 401로 바뀌는 문제를 잡지 못한다.
// [권한]  * 그래서 이 테스트만 진짜 HTTP 요청을 보낸다.
// [권한]  */
// [권한] @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
// [권한] class SecurityHttpStatusTest {
// [권한]
// [권한]     @LocalServerPort int port;
// [권한]     @Autowired JwtProvider jwtProvider;
// [권한]
// [권한]     private final HttpClient client = HttpClient.newHttpClient();
// [권한]
// [권한]     private int statusOf(String path, String token) throws Exception {
// [권한]         HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET();
// [권한]         if (token != null) {
// [권한]             request.header("Authorization", "Bearer " + token);
// [권한]         }
// [권한]         return client.send(request.build(), HttpResponse.BodyHandlers.discarding()).statusCode();
// [권한]     }
// [권한]
// [권한]     @Test
// [권한]     @DisplayName("일반 사용자가 관리자 API에 접근하면 실제 서버에서도 403 (401로 바뀌지 않는다)")
// [권한]     void userGetsForbidden() throws Exception {
// [권한]         assertThat(statusOf("/api/admin/medicines", jwtProvider.createToken(1L, Authority.USER))).isEqualTo(403);
// [권한]     }
// [권한]
// [권한]     @Test
// [권한]     @DisplayName("토큰이 없으면 401")
// [권한]     void anonymousGetsUnauthorized() throws Exception {
// [권한]         assertThat(statusOf("/api/admin/medicines", null)).isEqualTo(401);
// [권한]         assertThat(statusOf("/api/my-medicines", null)).isEqualTo(401);
// [권한]     }
// [권한]
// [권한]     @Test
// [권한]     @DisplayName("ADMIN 토큰이면 통과")
// [권한]     void adminAllowed() throws Exception {
// [권한]         assertThat(statusOf("/api/admin/medicines", jwtProvider.createToken(1L, Authority.ADMIN))).isEqualTo(200);
// [권한]     }
// [권한] }
