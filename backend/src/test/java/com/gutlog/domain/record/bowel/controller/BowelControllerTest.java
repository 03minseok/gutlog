package com.gutlog.domain.record.bowel.controller;

import com.gutlog.domain.record.bowel.repository.BowelRecordRepository;
import com.gutlog.domain.user.repository.UserRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * BowelController 통합 테스트.
 * 실제 요청 흐름과 같도록 테스트 클래스에 @Transactional을 걸지 않는다.
 * 대신 테스트가 끝날 때마다 FK 순서대로 데이터를 직접 지운다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class BowelControllerTest {

    private static final String BASE = "/api/record/bowel";

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired BowelRecordRepository bowelRecordRepository;

    private String myToken;
    private String otherToken;

    @BeforeEach
    void setUp() throws Exception {
        myToken = signupAndLogin("boweluser", "배변러");
        otherToken = signupAndLogin("otheruser", "남남");
    }

    @AfterEach
    void cleanUp() {
        bowelRecordRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
    }

    private String signupAndLogin(String username, String nickname) throws Exception {
        mockMvc.perform(post("/api/users/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username": "%s", "password": "password123", "nickname": "%s", "role": "GUARDIAN"}
                                """.formatted(username, nickname)))
                .andExpect(status().isOk());
        String body = mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username": "%s", "password": "password123"}
                                """.formatted(username)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(body, "$.data.accessToken");
    }

    private static String bowelJson(String bowelAt, boolean bleeding, String bowelCondition, int painLevel, String memo) {
        String memoValue = memo == null ? "null" : "\"" + memo + "\"";
        return """
                {"bowelAt": "%s", "bleeding": %s, "bowelCondition": "%s", "painLevel": %d, "memo": %s}
                """.formatted(bowelAt, bleeding, bowelCondition, painLevel, memoValue);
    }

    private static String bowelJson(String bowelAt) {
        return bowelJson(bowelAt, false, "TYPE_4", 0, null);
    }

    private ResultActions create(String token, String json) throws Exception {
        return mockMvc.perform(post(BASE)
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    private long createAndGetId(String token, String json) throws Exception {
        String body = create(token, json).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.data.id")).longValue();
    }

    @Nested
    @DisplayName("POST /api/record/bowel")
    class Create {

        @Test
        @DisplayName("성공 - 모든 필드를 저장하고 그대로 반환한다")
        void success() throws Exception {
            create(myToken, bowelJson("2026-10-06T08:30:00", true, "TYPE_6", 7, "아침에 복통"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").isNumber())
                    .andExpect(jsonPath("$.data.bowelAt").value("2026-10-06T08:30:00"))
                    .andExpect(jsonPath("$.data.bleeding").value(true))
                    .andExpect(jsonPath("$.data.bowelCondition").value("TYPE_6"))
                    .andExpect(jsonPath("$.data.painLevel").value(7))
                    .andExpect(jsonPath("$.data.memo").value("아침에 복통"));

            assertThat(bowelRecordRepository.count()).isEqualTo(1);
        }

        @Test
        @DisplayName("성공 - 메모는 생략할 수 있다")
        void withoutMemo() throws Exception {
            create(myToken, """
                    {"bowelAt": "2026-10-06T08:30:00", "bleeding": false, "bowelCondition": "TYPE_4", "painLevel": 0}
                    """)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.memo").value(nullValue()));
        }

        @Test
        @DisplayName("실패 - 토큰이 없으면 401")
        void unauthorized() throws Exception {
            mockMvc.perform(post(BASE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(bowelJson("2026-10-06T08:30:00")))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("실패 - 배변 시간이 없으면 400")
        void noBowelAt() throws Exception {
            create(myToken, """
                    {"bleeding": false, "bowelCondition": "TYPE_4", "painLevel": 0}
                    """)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("배변 시간은 필수입니다."));
        }

        @Test
        @DisplayName("실패 - 배변 상태가 없으면 400")
        void noBowelCondition() throws Exception {
            create(myToken, """
                    {"bowelAt": "2026-10-06T08:30:00", "bleeding": false, "painLevel": 0}
                    """)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("배변 상태는 필수입니다."));
        }

        @Test
        @DisplayName("실패 - 통증 정도가 없으면 400")
        void noPainLevel() throws Exception {
            create(myToken, """
                    {"bowelAt": "2026-10-06T08:30:00", "bleeding": false, "bowelCondition": "TYPE_4"}
                    """)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("통증 정도는 필수입니다."));
        }

        @Test
        @DisplayName("실패 - 통증이 0~10 범위를 벗어나면 400")
        void painOutOfRange() throws Exception {
            create(myToken, bowelJson("2026-10-06T08:30:00", false, "TYPE_4", 11, null))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("통증은 10 이하여야 합니다."));
            create(myToken, bowelJson("2026-10-06T08:30:00", false, "TYPE_4", -1, null))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("통증은 0 이상이어야 합니다."));

            assertThat(bowelRecordRepository.count()).isZero();
        }

        @Test
        @DisplayName("실패 - 메모가 500자를 넘으면 400")
        void memoTooLong() throws Exception {
            create(myToken, bowelJson("2026-10-06T08:30:00", false, "TYPE_4", 0, "가".repeat(501)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("메모는 최대 500자까지 입력 가능합니다."));
        }

        @Test
        @DisplayName("실패 - 없는 배변 상태 값(TYPE_8)이면 400")
        void invalidBowelCondition() throws Exception {
            create(myToken, bowelJson("2026-10-06T08:30:00", false, "TYPE_8", 0, null))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("잘못된 요청입니다."));
        }

        @Test
        @DisplayName("실패 - 배변 시간 형식이 잘못되면 400")
        void invalidBowelAtFormat() throws Exception {
            create(myToken, bowelJson("2026/10/06 08:30"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/record/bowel/{id}")
    class GetOne {

        @Test
        @DisplayName("성공 - 내 배변 기록을 조회한다")
        void success() throws Exception {
            long id = createAndGetId(myToken, bowelJson("2026-10-06T08:30:00", true, "TYPE_2", 3, "메모"));

            mockMvc.perform(get(BASE + "/" + id).header("Authorization", myToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value(id))
                    .andExpect(jsonPath("$.data.bowelAt").value("2026-10-06T08:30:00"))
                    .andExpect(jsonPath("$.data.bleeding").value(true))
                    .andExpect(jsonPath("$.data.bowelCondition").value("TYPE_2"))
                    .andExpect(jsonPath("$.data.painLevel").value(3))
                    .andExpect(jsonPath("$.data.memo").value("메모"));
        }

        @Test
        @DisplayName("실패 - 남의 배변 기록은 404 (존재 여부를 숨긴다)")
        void othersRecord() throws Exception {
            long id = createAndGetId(myToken, bowelJson("2026-10-06T08:30:00"));

            mockMvc.perform(get(BASE + "/" + id).header("Authorization", otherToken))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("배변 기록을 찾을 수 없습니다."));
        }

        @Test
        @DisplayName("실패 - 없는 배변 기록은 404")
        void notFound() throws Exception {
            mockMvc.perform(get(BASE + "/999999").header("Authorization", myToken))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("배변 기록을 찾을 수 없습니다."));
        }

        @Test
        @DisplayName("실패 - id가 숫자가 아니면 400")
        void invalidId() throws Exception {
            mockMvc.perform(get(BASE + "/abc").header("Authorization", myToken))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/record/bowel?from&to")
    class GetByPeriod {

        @Test
        @DisplayName("성공 - 기간 안의 내 기록만 시간순으로 반환한다 (from, to 모두 포함)")
        void success() throws Exception {
            createAndGetId(myToken, bowelJson("2026-10-06T23:59:59"));
            createAndGetId(myToken, bowelJson("2026-10-05T00:00:00"));
            createAndGetId(myToken, bowelJson("2026-10-07T00:00:00"));   // 기간 밖
            createAndGetId(myToken, bowelJson("2026-10-04T23:59:59"));   // 기간 밖
            createAndGetId(otherToken, bowelJson("2026-10-06T12:00:00")); // 남의 기록

            mockMvc.perform(get(BASE).header("Authorization", myToken)
                            .param("from", "2026-10-05")
                            .param("to", "2026-10-06"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(2)))
                    .andExpect(jsonPath("$.data[0].bowelAt").value("2026-10-05T00:00:00"))
                    .andExpect(jsonPath("$.data[1].bowelAt").value("2026-10-06T23:59:59"));
        }

        @Test
        @DisplayName("from과 to가 같으면 그날 하루만 조회한다")
        void sameDay() throws Exception {
            createAndGetId(myToken, bowelJson("2026-10-06T08:00:00"));
            createAndGetId(myToken, bowelJson("2026-10-07T08:00:00"));

            mockMvc.perform(get(BASE).header("Authorization", myToken)
                            .param("from", "2026-10-06")
                            .param("to", "2026-10-06"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(1)))
                    .andExpect(jsonPath("$.data[0].bowelAt").value("2026-10-06T08:00:00"));
        }

        @Test
        @DisplayName("기록이 없으면 빈 배열")
        void empty() throws Exception {
            mockMvc.perform(get(BASE).header("Authorization", myToken)
                            .param("from", "2026-10-01")
                            .param("to", "2026-10-01"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(0)));
        }

        @Test
        @DisplayName("실패 - from이 to보다 늦으면 400")
        void invalidRange() throws Exception {
            mockMvc.perform(get(BASE).header("Authorization", myToken)
                            .param("from", "2026-10-07")
                            .param("to", "2026-10-06"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("날짜 범위가 올바르지 않습니다."));
        }

        @Test
        @DisplayName("실패 - from/to가 없으면 400")
        void missingParams() throws Exception {
            mockMvc.perform(get(BASE).header("Authorization", myToken))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("실패 - 날짜 형식이 잘못되면 400")
        void invalidDateFormat() throws Exception {
            mockMvc.perform(get(BASE).header("Authorization", myToken)
                            .param("from", "2026/10/05")
                            .param("to", "2026-10-06"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("PUT /api/record/bowel/{id}")
    class Edit {

        @Test
        @DisplayName("성공 - 모든 필드를 통째로 바꾸고 DB에도 반영된다")
        void success() throws Exception {
            long id = createAndGetId(myToken, bowelJson("2026-10-06T08:30:00", false, "TYPE_4", 0, "처음"));

            mockMvc.perform(put(BASE + "/" + id)
                            .header("Authorization", myToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(bowelJson("2026-10-06T09:00:00", true, "TYPE_7", 8, "수정")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value(id))
                    .andExpect(jsonPath("$.data.bowelAt").value("2026-10-06T09:00:00"))
                    .andExpect(jsonPath("$.data.bleeding").value(true))
                    .andExpect(jsonPath("$.data.bowelCondition").value("TYPE_7"))
                    .andExpect(jsonPath("$.data.painLevel").value(8))
                    .andExpect(jsonPath("$.data.memo").value("수정"));

            // 다시 조회해도 바뀐 값이 DB에 반영되어 있어야 한다 (더티 체킹)
            mockMvc.perform(get(BASE + "/" + id).header("Authorization", myToken))
                    .andExpect(jsonPath("$.data.bowelAt").value("2026-10-06T09:00:00"))
                    .andExpect(jsonPath("$.data.bleeding").value(true))
                    .andExpect(jsonPath("$.data.bowelCondition").value("TYPE_7"))
                    .andExpect(jsonPath("$.data.painLevel").value(8))
                    .andExpect(jsonPath("$.data.memo").value("수정"));
            assertThat(bowelRecordRepository.count()).isEqualTo(1);
        }

        @Test
        @DisplayName("성공 - 메모를 빼고 보내면 메모가 지워진다 (전체 교체)")
        void clearMemo() throws Exception {
            long id = createAndGetId(myToken, bowelJson("2026-10-06T08:30:00", false, "TYPE_4", 0, "지울 메모"));

            mockMvc.perform(put(BASE + "/" + id)
                            .header("Authorization", myToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(bowelJson("2026-10-06T08:30:00", false, "TYPE_4", 0, null)))
                    .andExpect(status().isOk());

            mockMvc.perform(get(BASE + "/" + id).header("Authorization", myToken))
                    .andExpect(jsonPath("$.data.memo").value(nullValue()));
        }

        @Test
        @DisplayName("실패 - 남의 기록은 수정할 수 없다 (404), 원본은 그대로")
        void othersRecord() throws Exception {
            long id = createAndGetId(myToken, bowelJson("2026-10-06T08:30:00", false, "TYPE_4", 0, null));

            mockMvc.perform(put(BASE + "/" + id)
                            .header("Authorization", otherToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(bowelJson("2026-10-06T09:00:00", true, "TYPE_7", 10, "해킹")))
                    .andExpect(status().isNotFound());

            mockMvc.perform(get(BASE + "/" + id).header("Authorization", myToken))
                    .andExpect(jsonPath("$.data.bowelAt").value("2026-10-06T08:30:00"))
                    .andExpect(jsonPath("$.data.bleeding").value(false))
                    .andExpect(jsonPath("$.data.painLevel").value(0));
        }

        @Test
        @DisplayName("실패 - 없는 기록은 404")
        void notFound() throws Exception {
            mockMvc.perform(put(BASE + "/999999")
                            .header("Authorization", myToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(bowelJson("2026-10-06T09:00:00")))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("실패 - 검증 실패 시 400, 기존 데이터는 그대로")
        void invalid() throws Exception {
            long id = createAndGetId(myToken, bowelJson("2026-10-06T08:30:00", false, "TYPE_4", 2, null));

            mockMvc.perform(put(BASE + "/" + id)
                            .header("Authorization", myToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(bowelJson("2026-10-06T09:00:00", false, "TYPE_4", 11, null)))
                    .andExpect(status().isBadRequest());

            mockMvc.perform(get(BASE + "/" + id).header("Authorization", myToken))
                    .andExpect(jsonPath("$.data.bowelAt").value("2026-10-06T08:30:00"))
                    .andExpect(jsonPath("$.data.painLevel").value(2));
        }
    }

    @Nested
    @DisplayName("DELETE /api/record/bowel/{id}")
    class Delete {

        @Test
        @DisplayName("성공 - 삭제하면 data는 null이고, 다시 조회하면 404")
        void success() throws Exception {
            long id = createAndGetId(myToken, bowelJson("2026-10-06T08:30:00"));

            mockMvc.perform(delete(BASE + "/" + id).header("Authorization", myToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").value(nullValue()));

            mockMvc.perform(get(BASE + "/" + id).header("Authorization", myToken))
                    .andExpect(status().isNotFound());
            assertThat(bowelRecordRepository.count()).isZero();
        }

        @Test
        @DisplayName("실패 - 남의 기록은 삭제할 수 없다 (404)")
        void othersRecord() throws Exception {
            long id = createAndGetId(myToken, bowelJson("2026-10-06T08:30:00"));

            mockMvc.perform(delete(BASE + "/" + id).header("Authorization", otherToken))
                    .andExpect(status().isNotFound());

            mockMvc.perform(get(BASE + "/" + id).header("Authorization", myToken))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("실패 - 없는 기록은 404")
        void notFound() throws Exception {
            mockMvc.perform(delete(BASE + "/999999").header("Authorization", myToken))
                    .andExpect(status().isNotFound());
        }
    }
}
