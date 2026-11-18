package com.gutlog.domain.record.weight.controller;

import com.gutlog.domain.record.weight.repository.WeightRecordRepository;
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

import java.time.Clock;
import java.time.LocalDate;

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
 * WeightController 통합 테스트.
 * 실제 요청 흐름과 같도록 테스트 클래스에 @Transactional을 걸지 않고, 테스트가 끝날 때마다 데이터를 직접 지운다.
 * "오늘"은 서버의 Clock 기준이다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class WeightControllerTest {

    private static final String BASE = "/api/record/weight";

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired WeightRecordRepository weightRecordRepository;
    @Autowired Clock clock;

    private String myToken;
    private String otherToken;
    private LocalDate today;

    @BeforeEach
    void setUp() throws Exception {
        myToken = signupAndLogin("weightuser", "체중러");
        otherToken = signupAndLogin("otheruser", "남남");
        today = LocalDate.now(clock);
    }

    @AfterEach
    void cleanUp() {
        weightRecordRepository.deleteAllInBatch();
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

    private ResultActions save(String token, Object date, String weightJson) throws Exception {
        return mockMvc.perform(put(BASE + "/" + date)
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"weight\": " + weightJson + "}"));
    }

    private ResultActions list(String token, Object from, Object to) throws Exception {
        return mockMvc.perform(get(BASE).header("Authorization", token)
                .param("from", from.toString()).param("to", to.toString()));
    }

    @Nested
    @DisplayName("PUT /api/record/weight/{date}")
    class Save {

        @Test
        @DisplayName("성공 - 없던 날짜는 새로 만들고 날짜와 체중을 반환한다")
        void create() throws Exception {
            save(myToken, today, "62.35")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.date").value(today.toString()))
                    .andExpect(jsonPath("$.data.weight").value(62.35));

            assertThat(weightRecordRepository.count()).isEqualTo(1);
        }

        @Test
        @DisplayName("성공 - 같은 날짜로 다시 등록하면 한 줄을 유지한 채 값만 바뀐다")
        void overwrite() throws Exception {
            save(myToken, today, "62.35").andExpect(status().isOk());
            save(myToken, today, "61.80")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.weight").value(61.8));

            assertThat(weightRecordRepository.count()).isEqualTo(1);
            list(myToken, today, today)
                    .andExpect(jsonPath("$.data", hasSize(1)))
                    .andExpect(jsonPath("$.data[0].weight").value(61.8));
        }

        @Test
        @DisplayName("성공 - 과거 날짜도 지정해서 등록할 수 있다")
        void pastDate() throws Exception {
            LocalDate past = today.minusDays(30);
            save(myToken, past, "70")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.date").value(past.toString()));
        }

        @Test
        @DisplayName("성공 - 다른 사용자는 같은 날짜에 따로 기록한다")
        void sameDateDifferentUsers() throws Exception {
            save(myToken, today, "60").andExpect(status().isOk());
            save(otherToken, today, "80").andExpect(status().isOk());

            assertThat(weightRecordRepository.count()).isEqualTo(2);
            list(myToken, today, today).andExpect(jsonPath("$.data[0].weight").value(60));
        }

        @Test
        @DisplayName("실패 400 - 미래 날짜")
        void futureDate() throws Exception {
            save(myToken, today.plusDays(1), "60")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("미래 날짜에는 기록할 수 없습니다."));

            assertThat(weightRecordRepository.count()).isZero();
        }

        @Test
        @DisplayName("실패 400 - 범위 밖 / 소수점 셋째 자리 / 누락")
        void invalidWeight() throws Exception {
            save(myToken, today, "0.99").andExpect(status().isBadRequest());
            save(myToken, today, "300.01").andExpect(status().isBadRequest());
            save(myToken, today, "62.345")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("체중은 소수점 둘째 자리까지 입력할 수 있습니다."));
            save(myToken, today, "null")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("체중은 필수입니다."));

            assertThat(weightRecordRepository.count()).isZero();
        }

        @Test
        @DisplayName("실패 400 - 날짜 형식이 잘못됨")
        void badDateFormat() throws Exception {
            save(myToken, "2026-13-01", "60").andExpect(status().isBadRequest());
            save(myToken, "20261001", "60").andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("실패 401 - 토큰 없음")
        void unauthorized() throws Exception {
            mockMvc.perform(put(BASE + "/" + today)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"weight\": 60}"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /api/record/weight")
    class Period {

        @Test
        @DisplayName("성공 - from, to를 모두 포함하고 날짜순으로, 내 기록만 준다")
        void success() throws Exception {
            LocalDate from = today.minusDays(6);
            save(myToken, today, "60.50");
            save(myToken, from, "60.00");
            save(myToken, today.minusDays(3), "60.25");
            save(myToken, from.minusDays(1), "59.00");    // 범위 밖
            save(otherToken, today.minusDays(3), "80");   // 남의 기록

            list(myToken, from, today)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(3)))
                    .andExpect(jsonPath("$.data[0].date").value(from.toString()))
                    .andExpect(jsonPath("$.data[0].weight").value(60.0))
                    .andExpect(jsonPath("$.data[1].date").value(today.minusDays(3).toString()))
                    .andExpect(jsonPath("$.data[1].weight").value(60.25))
                    .andExpect(jsonPath("$.data[2].date").value(today.toString()));
        }

        @Test
        @DisplayName("성공 - 기록이 없으면 빈 배열")
        void empty() throws Exception {
            list(myToken, today, today)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(0)));
        }

        @Test
        @DisplayName("실패 400 - from이 to보다 늦음")
        void invalidRange() throws Exception {
            list(myToken, today, today.minusDays(1))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("날짜 범위가 올바르지 않습니다."));
        }

        @Test
        @DisplayName("실패 400 - 파라미터 누락")
        void missingParam() throws Exception {
            mockMvc.perform(get(BASE).header("Authorization", myToken).param("from", today.toString()))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("DELETE /api/record/weight/{date}")
    class Delete {

        @Test
        @DisplayName("성공 - 그 날짜 기록을 지우고 data는 null")
        void success() throws Exception {
            save(myToken, today, "60").andExpect(status().isOk());

            mockMvc.perform(delete(BASE + "/" + today).header("Authorization", myToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").value(nullValue()));

            assertThat(weightRecordRepository.count()).isZero();
        }

        @Test
        @DisplayName("성공 - 지운 날짜에 다시 등록할 수 있다")
        void recreateAfterDelete() throws Exception {
            save(myToken, today, "60").andExpect(status().isOk());
            mockMvc.perform(delete(BASE + "/" + today).header("Authorization", myToken)).andExpect(status().isOk());

            save(myToken, today, "61").andExpect(status().isOk());
            assertThat(weightRecordRepository.count()).isEqualTo(1);
        }

        @Test
        @DisplayName("실패 404 - 그 날짜에 기록이 없음")
        void notFound() throws Exception {
            mockMvc.perform(delete(BASE + "/" + today).header("Authorization", myToken))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("체중 기록을 찾을 수 없습니다."));
        }

        @Test
        @DisplayName("실패 404 - 남의 기록만 있는 날짜는 지울 수 없고, 남의 기록은 남는다")
        void othersRecord() throws Exception {
            save(otherToken, today, "80").andExpect(status().isOk());

            mockMvc.perform(delete(BASE + "/" + today).header("Authorization", myToken))
                    .andExpect(status().isNotFound());

            assertThat(weightRecordRepository.count()).isEqualTo(1);
        }
    }
}
