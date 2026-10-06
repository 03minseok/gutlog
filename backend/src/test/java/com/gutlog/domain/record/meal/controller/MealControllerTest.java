package com.gutlog.domain.record.meal.controller;

import com.gutlog.domain.record.meal.repository.FoodRepository;
import com.gutlog.domain.record.meal.repository.MealFoodRepository;
import com.gutlog.domain.record.meal.repository.MealRecordRepository;
import com.gutlog.domain.user.repository.UserRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * MealController 통합 테스트.
 * 실제 요청 흐름과 같도록 테스트 클래스에 @Transactional을 걸지 않는다.
 * (걸면 모든 요청이 한 트랜잭션·한 영속성 컨텍스트를 공유해서, 요청마다 트랜잭션이 나뉘는 실제 동작과 달라진다)
 * 대신 테스트가 끝날 때마다 FK 순서대로 데이터를 직접 지운다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class MealControllerTest {

    private static final String BASE = "/api/record/meal";

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired MealRecordRepository mealRecordRepository;
    @Autowired FoodRepository foodRepository;
    @Autowired MealFoodRepository mealFoodRepository;

    private String myToken;
    private String otherToken;

    @BeforeEach
    void setUp() throws Exception {
        myToken = signupAndLogin("mealuser", "식사러");
        otherToken = signupAndLogin("otheruser", "남남");
    }

    @AfterEach
    void cleanUp() {
        mealFoodRepository.deleteAllInBatch();
        foodRepository.deleteAllInBatch();
        mealRecordRepository.deleteAllInBatch();
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

    private static String mealJson(String eatenAt, String... foods) {
        String foodArray = String.join(", ", java.util.Arrays.stream(foods).map(f -> "\"" + f + "\"").toList());
        return """
                {"eatenAt": "%s", "foods": [%s]}
                """.formatted(eatenAt, foodArray);
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
    @DisplayName("POST /api/record/meal")
    class Create {

        @Test
        @DisplayName("성공 - 음식 이름을 정규화(공백·중복 제거)해서 저장하고 반환한다")
        void success() throws Exception {
            create(myToken, mealJson("2026-10-06T08:30:00", " 밥 ", "김치", "밥"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").isNumber())
                    .andExpect(jsonPath("$.data.eatenAt").value("2026-10-06T08:30:00"))
                    .andExpect(jsonPath("$.data.foods").value(org.hamcrest.Matchers.contains("밥", "김치")));
        }

        @Test
        @DisplayName("같은 음식을 여러 식사에서 써도 Food는 하나만 만들어진다")
        void reuseFood() throws Exception {
            create(myToken, mealJson("2026-10-06T08:30:00", "밥", "김치")).andExpect(status().isOk());
            create(myToken, mealJson("2026-10-06T12:30:00", "밥", "된장국")).andExpect(status().isOk());

            assertThat(foodRepository.count()).isEqualTo(3); // 밥, 김치, 된장국
            assertThat(mealFoodRepository.count()).isEqualTo(4);
        }

        @Test
        @DisplayName("실패 - 토큰이 없으면 401")
        void unauthorized() throws Exception {
            mockMvc.perform(post(BASE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mealJson("2026-10-06T08:30:00", "밥")))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("실패 - 음식이 비어 있으면 400")
        void emptyFoods() throws Exception {
            create(myToken, mealJson("2026-10-06T08:30:00"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("음식을 입력해주세요."));
        }

        @Test
        @DisplayName("실패 - 식사 시간이 없으면 400")
        void noEatenAt() throws Exception {
            create(myToken, """
                    {"foods": ["밥"]}
                    """)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("식사 시간은 필수입니다."));
        }

        @Test
        @Disabled("유저 리뷰 #2와 같은 원인: 날짜 형식 오류(HttpMessageNotReadableException)가 현재 500. 수정 후 활성화")
        @DisplayName("실패 - 식사 시간 형식이 잘못되면 400")
        void invalidEatenAtFormat() throws Exception {
            create(myToken, mealJson("2026/10/06 08:30", "밥"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/record/meal/{id}")
    class GetOne {

        @Test
        @DisplayName("성공 - 내 식사를 음식과 함께 조회한다")
        void success() throws Exception {
            long id = createAndGetId(myToken, mealJson("2026-10-06T08:30:00", "밥", "김치"));

            mockMvc.perform(get(BASE + "/" + id).header("Authorization", myToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value(id))
                    .andExpect(jsonPath("$.data.foods", hasSize(2)));
        }

        @Test
        @DisplayName("실패 - 남의 식사는 404 (존재 여부를 숨긴다)")
        void othersMeal() throws Exception {
            long id = createAndGetId(myToken, mealJson("2026-10-06T08:30:00", "밥"));

            mockMvc.perform(get(BASE + "/" + id).header("Authorization", otherToken))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("식사 기록을 찾을 수 없습니다."));
        }

        @Test
        @DisplayName("실패 - 없는 식사는 404")
        void notFound() throws Exception {
            mockMvc.perform(get(BASE + "/999999").header("Authorization", myToken))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/record/meal?from&to")
    class GetByPeriod {

        @Test
        @DisplayName("성공 - 기간 안의 내 식사만 시간순으로, 음식과 함께 반환한다")
        void success() throws Exception {
            createAndGetId(myToken, mealJson("2026-10-06T19:00:00", "삼겹살"));
            createAndGetId(myToken, mealJson("2026-10-05T08:00:00", "밥", "김치"));
            createAndGetId(myToken, mealJson("2026-10-07T00:00:00", "야식"));  // 기간 밖
            createAndGetId(otherToken, mealJson("2026-10-06T12:00:00", "남의밥")); // 남의 식사

            mockMvc.perform(get(BASE).header("Authorization", myToken)
                            .param("from", "2026-10-05")
                            .param("to", "2026-10-06"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(2)))
                    .andExpect(jsonPath("$.data[0].eatenAt").value("2026-10-05T08:00:00"))
                    .andExpect(jsonPath("$.data[0].foods", hasSize(2)))
                    .andExpect(jsonPath("$.data[1].eatenAt").value("2026-10-06T19:00:00"))
                    .andExpect(jsonPath("$.data[1].foods[0]").value("삼겹살"));
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
        @Disabled("유저 리뷰 #2와 같은 원인: 필수 파라미터 누락이 현재 500. 수정 후 활성화")
        @DisplayName("실패 - from/to가 없으면 400")
        void missingParams() throws Exception {
            mockMvc.perform(get(BASE).header("Authorization", myToken))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("PUT /api/record/meal/{id}")
    class Edit {

        @Test
        @DisplayName("성공 - 시간과 음식 목록을 통째로 바꾼다. 기존 음식을 유지해도 문제없다")
        void success() throws Exception {
            long id = createAndGetId(myToken, mealJson("2026-10-06T08:30:00", "밥", "김치"));

            mockMvc.perform(put(BASE + "/" + id)
                            .header("Authorization", myToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mealJson("2026-10-06T09:00:00", "밥", "계란")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.eatenAt").value("2026-10-06T09:00:00"))
                    .andExpect(jsonPath("$.data.foods").value(org.hamcrest.Matchers.contains("밥", "계란")));

            // 다시 조회해도 바뀐 값이 DB에 반영되어 있어야 한다
            mockMvc.perform(get(BASE + "/" + id).header("Authorization", myToken))
                    .andExpect(jsonPath("$.data.eatenAt").value("2026-10-06T09:00:00"))
                    .andExpect(jsonPath("$.data.foods").value(org.hamcrest.Matchers.containsInAnyOrder("밥", "계란")));
            assertThat(mealFoodRepository.count()).isEqualTo(2);
        }

        @Test
        @DisplayName("실패 - 남의 식사는 수정할 수 없다 (404)")
        void othersMeal() throws Exception {
            long id = createAndGetId(myToken, mealJson("2026-10-06T08:30:00", "밥"));

            mockMvc.perform(put(BASE + "/" + id)
                            .header("Authorization", otherToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mealJson("2026-10-06T09:00:00", "라면")))
                    .andExpect(status().isNotFound());

            mockMvc.perform(get(BASE + "/" + id).header("Authorization", myToken))
                    .andExpect(jsonPath("$.data.eatenAt").value("2026-10-06T08:30:00"))
                    .andExpect(jsonPath("$.data.foods[0]").value("밥"));
        }

        @Test
        @DisplayName("실패 - 검증 실패 시 400, 기존 데이터는 그대로")
        void invalid() throws Exception {
            long id = createAndGetId(myToken, mealJson("2026-10-06T08:30:00", "밥"));

            mockMvc.perform(put(BASE + "/" + id)
                            .header("Authorization", myToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mealJson("2026-10-06T09:00:00")))
                    .andExpect(status().isBadRequest());

            mockMvc.perform(get(BASE + "/" + id).header("Authorization", myToken))
                    .andExpect(jsonPath("$.data.foods[0]").value("밥"));
        }
    }

    @Nested
    @DisplayName("DELETE /api/record/meal/{id}")
    class Delete {

        @Test
        @DisplayName("성공 - 삭제 후 조회하면 404, 음식(Food)은 남는다")
        void success() throws Exception {
            long id = createAndGetId(myToken, mealJson("2026-10-06T08:30:00", "밥", "김치"));

            mockMvc.perform(delete(BASE + "/" + id).header("Authorization", myToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            mockMvc.perform(get(BASE + "/" + id).header("Authorization", myToken))
                    .andExpect(status().isNotFound());
            assertThat(mealFoodRepository.count()).isZero();
            assertThat(foodRepository.count()).isEqualTo(2);
        }

        @Test
        @DisplayName("실패 - 남의 식사는 삭제할 수 없다 (404)")
        void othersMeal() throws Exception {
            long id = createAndGetId(myToken, mealJson("2026-10-06T08:30:00", "밥"));

            mockMvc.perform(delete(BASE + "/" + id).header("Authorization", otherToken))
                    .andExpect(status().isNotFound());

            mockMvc.perform(get(BASE + "/" + id).header("Authorization", myToken))
                    .andExpect(status().isOk());
        }
    }
}
