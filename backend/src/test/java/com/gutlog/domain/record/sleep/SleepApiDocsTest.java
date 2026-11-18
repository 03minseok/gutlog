package com.gutlog.domain.record.sleep;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 수면 요청의 API 명세(/v3/api-docs) 테스트.
 * 프론트는 명세를 기준으로 입력 규칙을 맞추므로, 30분 단위·범위·필수 조건이 명세에 드러나는지 확인한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SleepApiDocsTest {

    private static final String SCHEMA = "$.components.schemas.SleepRequest";

    @Autowired MockMvc mockMvc;

    @Test
    @DisplayName("sleepMinutes는 필수, 30 ~ 1440, 30의 배수로 명세에 나온다")
    void sleepMinutesRules() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(SCHEMA + ".required", hasItem("sleepMinutes")))
                .andExpect(jsonPath(SCHEMA + ".properties.sleepMinutes.type").value("integer"))
                .andExpect(jsonPath(SCHEMA + ".properties.sleepMinutes.minimum").value(30))
                .andExpect(jsonPath(SCHEMA + ".properties.sleepMinutes.maximum").value(1440))
                .andExpect(jsonPath(SCHEMA + ".properties.sleepMinutes.multipleOf").value(30));
    }

    @Test
    @DisplayName("검증용 메서드(isHalfHourUnit)는 요청 필드로 명세에 나오지 않는다")
    void validationMethodHidden() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(SCHEMA + ".properties.halfHourUnit").doesNotExist())
                .andExpect(jsonPath(SCHEMA + ".required", not(hasItem("halfHourUnit"))));
    }
}
