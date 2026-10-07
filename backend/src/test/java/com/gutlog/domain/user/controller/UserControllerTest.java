package com.gutlog.domain.user.controller;

import com.gutlog.global.config.JwtProvider;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * UserController 통합 테스트.
 * 실제 스프링 컨텍스트(시큐리티 필터, 예외 핸들러, JPA, 인메모리 H2)를 모두 띄우고
 * HTTP 요청 → 응답 JSON까지 끝에서 끝으로 검증한다. @Transactional로 테스트마다 롤백된다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired JwtProvider jwtProvider;

    private static final String PATIENT_SIGNUP = """
            {
              "username": "gutlog",
              "password": "password123",
              "nickname": "장건강",
              "role": "PATIENT",
              "ibdType": "ULCERATIVE_COLITIS"
            }
            """;

    private ResultActions signup(String json) throws Exception {
        return mockMvc.perform(post("/api/users/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    private ResultActions login(String username, String password) throws Exception {
        return mockMvc.perform(post("/api/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"username": "%s", "password": "%s"}
                        """.formatted(username, password)));
    }

    private String signupAndLogin() throws Exception {
        signup(PATIENT_SIGNUP).andExpect(status().isOk());
        String body = login("gutlog", "password123")
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.data.accessToken");
    }

    @Nested
    @DisplayName("POST /api/users/signup")
    class Signup {

        @Test
        @DisplayName("성공 - 200과 유저 정보를 반환하고, 비밀번호는 응답에 포함되지 않는다")
        void success() throws Exception {
            signup(PATIENT_SIGNUP)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").isNumber())
                    .andExpect(jsonPath("$.data.username").value("gutlog"))
                    .andExpect(jsonPath("$.data.nickname").value("장건강"))
                    .andExpect(jsonPath("$.data.role").value("PATIENT"))
                    .andExpect(jsonPath("$.data.ibdType").value("ULCERATIVE_COLITIS"))
                    .andExpect(jsonPath("$.data.password").doesNotExist());
        }

        @Test
        @DisplayName("실패 - 아이디 중복이면 409")
        void duplicateUsername() throws Exception {
            signup(PATIENT_SIGNUP).andExpect(status().isOk());

            signup(PATIENT_SIGNUP.replace("장건강", "다른닉"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("이미 사용 중인 아이디입니다."));
        }

        @Test
        @DisplayName("실패 - 환자인데 질환 종류가 없으면 400")
        void patientWithoutIbdType() throws Exception {
            signup("""
                    {"username": "gutlog", "password": "password123", "nickname": "장건강", "role": "PATIENT"}
                    """)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("환자는 질환 종류를 선택해야 합니다."));
        }

        @Test
        @DisplayName("실패 - 비밀번호가 너무 짧으면 400")
        void shortPassword() throws Exception {
            signup(PATIENT_SIGNUP.replace("password123", "abc1234"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("비밀번호는 8자 이상 20자 이하로 입력해주세요."));
        }

        @Test
        @DisplayName("실패 - 닉네임 중복이면 409")
        void duplicateNickname() throws Exception {
            signup(PATIENT_SIGNUP).andExpect(status().isOk());

            signup(PATIENT_SIGNUP.replace("\"gutlog\"", "\"other\""))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 role 값이면 400")
        void invalidRole() throws Exception {
            signup(PATIENT_SIGNUP.replace("PATIENT", "ADMIN"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    @Nested
    @DisplayName("GET /api/users/check-username")
    class CheckUsername {

        @Test
        @DisplayName("가입되지 않은 아이디면 true")
        void available() throws Exception {
            mockMvc.perform(get("/api/users/check-username").param("username", "newbie"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").value(true));
        }

        @Test
        @DisplayName("이미 가입된 아이디면 false")
        void taken() throws Exception {
            signup(PATIENT_SIGNUP).andExpect(status().isOk());

            mockMvc.perform(get("/api/users/check-username").param("username", "gutlog"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").value(false));
        }

        @Test
        @DisplayName("username 파라미터가 없으면 400")
        void missingParam() throws Exception {
            mockMvc.perform(get("/api/users/check-username"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/users/login")
    class Login {

        @Test
        @DisplayName("성공 - 토큰과 유저 정보를 반환한다")
        void success() throws Exception {
            signup(PATIENT_SIGNUP).andExpect(status().isOk());

            login("gutlog", "password123")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.accessToken").isString())
                    .andExpect(jsonPath("$.data.user.username").value("gutlog"));
        }

        @Test
        @DisplayName("실패 - 비밀번호가 틀리면 401")
        void wrongPassword() throws Exception {
            signup(PATIENT_SIGNUP).andExpect(status().isOk());

            login("gutlog", "wrongpassword")
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("아이디 또는 비밀번호가 올바르지 않습니다."));
        }

        @Test
        @DisplayName("실패 - 없는 아이디면 401 (비밀번호 틀림과 같은 메시지)")
        void unknownUser() throws Exception {
            login("nobody", "password123")
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("아이디 또는 비밀번호가 올바르지 않습니다."));
        }
    }

    @Nested
    @DisplayName("GET /api/users/me")
    class Me {

        @Test
        @DisplayName("성공 - 로그인 토큰으로 내 정보를 조회한다")
        void success() throws Exception {
            String token = signupAndLogin();

            mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.username").value("gutlog"))
                    .andExpect(jsonPath("$.data.nickname").value("장건강"));
        }

        @Test
        @DisplayName("실패 - 토큰이 없으면 401")
        void noToken() throws Exception {
            mockMvc.perform(get("/api/users/me"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("실패 - 위조된 토큰이면 401")
        void invalidToken() throws Exception {
            mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer not-a-jwt"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("실패 - Bearer 접두사가 없으면 401")
        void noBearerPrefix() throws Exception {
            String token = signupAndLogin();

            mockMvc.perform(get("/api/users/me").header("Authorization", token))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("실패 - 토큰은 유효하지만 유저가 없으면 404")
        void userDeleted() throws Exception {
            String token = jwtProvider.createToken(999_999L);

            mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer " + token))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("사용자를 찾을 수 없습니다."));
        }
    }
}
