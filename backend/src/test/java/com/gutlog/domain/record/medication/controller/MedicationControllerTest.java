package com.gutlog.domain.record.medication.controller;

import com.gutlog.domain.record.medication.repository.MedicationLogRepository;
import com.gutlog.domain.record.medication.repository.UserMedicineRepository;
import com.gutlog.domain.record.medication.entity.DosageForm;
import com.gutlog.domain.record.medication.repository.MedicineRepository;
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
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 복용 약·복용 기록 통합 테스트. 서버 시작 시 들어간 초기 약 목록을 쓴다.
 * 날짜는 서버의 Clock 기준 "오늘"로 둔다. (미래 날짜를 막고, 등록일 이후만 보여주므로)
 */
@SpringBootTest
@AutoConfigureMockMvc
class MedicationControllerTest {

    private static final String MY = "/api/my-medicines";
    private static final String RECORD = "/api/record/medication";

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired MedicineRepository medicineRepository;
    @Autowired UserMedicineRepository userMedicineRepository;
    @Autowired MedicationLogRepository medicationLogRepository;
    @Autowired Clock clock;

    private String myToken;
    private String otherToken;
    private LocalDate today;
    private long pentasaTabletId;
    private long humiraId;
    private long loperamideId;

    @BeforeEach
    void setUp() throws Exception {
        myToken = signupAndLogin("meduser");
        otherToken = signupAndLogin("otheruser");
        today = LocalDate.now(clock);
        pentasaTabletId = medicineId("메살라진", "펜타사", DosageForm.TABLET);
        humiraId = medicineId("아달리무맙", "휴미라", DosageForm.SC_INJECTION);
        loperamideId = medicineId("로페라미드", "로프민", DosageForm.CAPSULE);
    }

    @AfterEach
    void cleanUp() {
        medicationLogRepository.deleteAllInBatch();
        userMedicineRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
    }

    private long medicineId(String generic, String brand, DosageForm form) {
        return medicineRepository.findByGenericNameAndBrandNameAndDosageForm(generic, brand, form).orElseThrow().getId();
    }

    private String signupAndLogin(String username) throws Exception {
        mockMvc.perform(post("/api/users/signup").contentType(MediaType.APPLICATION_JSON).content("""
                        {"username": "%s", "password": "password123", "nickname": "%s", "role": "PATIENT", "ibdType": "CROHNS_DISEASE"}
                        """.formatted(username, username.substring(0, 5))))
                .andExpect(status().isOk());
        String body = mockMvc.perform(post("/api/users/login").contentType(MediaType.APPLICATION_JSON).content("""
                        {"username": "%s", "password": "password123"}
                        """.formatted(username)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(body, "$.data.accessToken");
    }

    private ResultActions register(String token, String json) throws Exception {
        return mockMvc.perform(post(MY).header("Authorization", token).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private long registerDaily(long medicineId, int count) throws Exception {
        return idOf(register(myToken, """
                {"medicineId": %d, "scheduleType": "DAILY", "dailyDoseCount": %d, "doseAmount": 1.5, "doseMemo": "식후"}
                """.formatted(medicineId, count)));
    }

    private long registerInterval(long medicineId, int days) throws Exception {
        return idOf(register(myToken, """
                {"medicineId": %d, "scheduleType": "INTERVAL", "intervalDays": %d}
                """.formatted(medicineId, days)));
    }

    private static long idOf(ResultActions result) throws Exception {
        String body = result.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.data.id")).longValue();
    }

    private ResultActions toggle(String token, long userMedicineId, LocalDate date, int dose) throws Exception {
        return mockMvc.perform(post(RECORD + "/%d/%s/%d/toggle".formatted(userMedicineId, date, dose)).header("Authorization", token));
    }

    private ResultActions daily(LocalDate date) throws Exception {
        return mockMvc.perform(get(RECORD + "/daily").param("date", date.toString()).header("Authorization", myToken));
    }

    @Nested
    @DisplayName("약 검색")
    class Search {

        @Test
        @DisplayName("성분명·제품명 일부로 찾고, 제형별로 따로 나온다")
        void search() throws Exception {
            mockMvc.perform(get("/api/medicines").param("keyword", "펜타사").header("Authorization", myToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(3)))  // 서방정, 서방과립, 좌약
                    .andExpect(jsonPath("$.data[*].doseUnit").value(org.hamcrest.Matchers.containsInAnyOrder("정", "포", "개")));
        }

        @Test
        @DisplayName("토큰이 없으면 401")
        void unauthorized() throws Exception {
            mockMvc.perform(get("/api/medicines")).andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("내 복용 약")
    class MyMedicines {

        @Test
        @DisplayName("등록하면 목록에 약 이름·단위와 함께 나온다")
        void registerAndList() throws Exception {
            registerDaily(pentasaTabletId, 3);

            mockMvc.perform(get(MY).header("Authorization", myToken))
                    .andExpect(jsonPath("$.data", hasSize(1)))
                    .andExpect(jsonPath("$.data[0].medicine.brandName").value("펜타사"))
                    .andExpect(jsonPath("$.data[0].medicine.doseUnit").value("정"))
                    .andExpect(jsonPath("$.data[0].scheduleType").value("DAILY"))
                    .andExpect(jsonPath("$.data[0].dailyDoseCount").value(3))
                    .andExpect(jsonPath("$.data[0].intervalDays").value(nullValue()))
                    .andExpect(jsonPath("$.data[0].doseAmount").value(1.5))
                    .andExpect(jsonPath("$.data[0].doseMemo").value("식후"));
        }

        @Test
        @DisplayName("같은 약을 활성 상태로 두 번 등록하면 409. 끊은 뒤에는 새로 등록할 수 있다")
        void duplicateThenReRegister() throws Exception {
            long first = registerDaily(pentasaTabletId, 3);
            register(myToken, """
                    {"medicineId": %d, "scheduleType": "DAILY", "dailyDoseCount": 1}
                    """.formatted(pentasaTabletId))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value("이미 복용 중인 약입니다."));

            mockMvc.perform(delete(MY + "/" + first).header("Authorization", myToken)).andExpect(status().isOk());
            long second = registerDaily(pentasaTabletId, 2);

            assertThat(second).isNotEqualTo(first);
            mockMvc.perform(get(MY).header("Authorization", myToken))
                    .andExpect(jsonPath("$.data[*].id").value(contains((int) second)));
        }

        @Test
        @DisplayName("복용 방식에 맞지 않는 값, 제형에 맞지 않는 복용량은 400")
        void invalid() throws Exception {
            register(myToken, """
                    {"medicineId": %d, "scheduleType": "AS_NEEDED", "dailyDoseCount": 3}
                    """.formatted(loperamideId))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("하루 복용 횟수는 매일 먹는 약에만 입력할 수 있습니다."));
            register(myToken, """
                    {"medicineId": %d, "scheduleType": "DAILY", "dailyDoseCount": 7}
                    """.formatted(pentasaTabletId))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("하루 복용 횟수는 1~6회입니다."));
            register(myToken, """
                    {"medicineId": %d, "scheduleType": "DAILY", "dailyDoseCount": 1, "doseAmount": 1.2}
                    """.formatted(pentasaTabletId))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("복용량이 올바르지 않습니다."));
            register(myToken, """
                    {"medicineId": 999999, "scheduleType": "AS_NEEDED"}
                    """)
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("수정 - 복용 방식을 바꿀 수 있고, 남의 약은 404")
        void edit() throws Exception {
            long id = registerDaily(humiraId, 1);

            mockMvc.perform(put(MY + "/" + id).header("Authorization", myToken).contentType(MediaType.APPLICATION_JSON).content("""
                            {"scheduleType": "INTERVAL", "intervalDays": 14, "doseAmount": 1}
                            """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.scheduleType").value("INTERVAL"))
                    .andExpect(jsonPath("$.data.dailyDoseCount").value(nullValue()))
                    .andExpect(jsonPath("$.data.intervalDays").value(14))
                    .andExpect(jsonPath("$.data.doseMemo").value(nullValue()));

            mockMvc.perform(put(MY + "/" + id).header("Authorization", otherToken).contentType(MediaType.APPLICATION_JSON).content("""
                            {"scheduleType": "AS_NEEDED"}
                            """))
                    .andExpect(status().isNotFound());
            mockMvc.perform(delete(MY + "/" + id).header("Authorization", otherToken)).andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("복용 체크 토글")
    class Toggle {

        @Test
        @DisplayName("회차 순서대로 체크하고, 역순으로 취소한다")
        void orderedToggle() throws Exception {
            long id = registerDaily(pentasaTabletId, 2);

            toggle(myToken, id, today, 2).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("이전 회차를 먼저 체크해 주세요."));
            toggle(myToken, id, today, 1).andExpect(status().isOk()).andExpect(jsonPath("$.data.taken").value(true));
            toggle(myToken, id, today, 2).andExpect(status().isOk()).andExpect(jsonPath("$.data.taken").value(true));
            toggle(myToken, id, today, 1).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("다음 회차를 먼저 취소해 주세요."));
            toggle(myToken, id, today, 2).andExpect(jsonPath("$.data.taken").value(false));
            toggle(myToken, id, today, 1).andExpect(jsonPath("$.data.taken").value(false));
        }

        @Test
        @DisplayName("취소했던 회차를 다시 체크하면 새 행 없이 되살아난다")
        void reCheckAfterSoftDelete() throws Exception {
            long id = registerDaily(pentasaTabletId, 1);
            toggle(myToken, id, today, 1).andExpect(jsonPath("$.data.taken").value(true));
            long logId = medicationLogRepository.findAll().getFirst().getId();
            toggle(myToken, id, today, 1).andExpect(jsonPath("$.data.taken").value(false));

            toggle(myToken, id, today, 1).andExpect(status().isOk()).andExpect(jsonPath("$.data.taken").value(true));

            assertThat(medicationLogRepository.count()).isEqualTo(1);
            assertThat(medicationLogRepository.findAll().getFirst().getId()).isEqualTo(logId);
            assertThat(medicationLogRepository.findAll().getFirst().isDeleted()).isFalse();
            daily(today).andExpect(jsonPath("$.data.medicines[0].takenDoses").value(contains(1)));
        }

        @Test
        @DisplayName("회차가 하루 복용 횟수보다 크면 400, INTERVAL·AS_NEEDED는 회차 1만")
        void doseOutOfRange() throws Exception {
            long dailyId = registerDaily(pentasaTabletId, 2);
            long intervalId = registerInterval(humiraId, 14);
            toggle(myToken, dailyId, today, 1);
            toggle(myToken, dailyId, today, 2);

            toggle(myToken, dailyId, today, 3).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("복용 회차가 올바르지 않습니다."));
            toggle(myToken, intervalId, today, 2).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("복용 회차가 올바르지 않습니다."));
        }

        @Test
        @DisplayName("미래 날짜는 400, 남의 약은 404, 날짜 형식이 틀리면 400")
        void rejected() throws Exception {
            long id = registerDaily(pentasaTabletId, 1);

            toggle(myToken, id, today.plusDays(1), 1).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("미래 날짜에는 기록할 수 없습니다."));
            toggle(otherToken, id, today, 1).andExpect(status().isNotFound());
            mockMvc.perform(post(RECORD + "/%d/2026-13-01/1/toggle".formatted(id)).header("Authorization", myToken))
                    .andExpect(status().isBadRequest());
            assertThat(medicationLogRepository.count()).isZero();
        }

        @Test
        @DisplayName("하루 복용 횟수를 줄여도 이미 남긴 기록은 그대로 남는다")
        void reduceCountKeepsLogs() throws Exception {
            long id = registerDaily(pentasaTabletId, 3);
            toggle(myToken, id, today, 1);
            toggle(myToken, id, today, 2);
            toggle(myToken, id, today, 3);

            mockMvc.perform(put(MY + "/" + id).header("Authorization", myToken).contentType(MediaType.APPLICATION_JSON).content("""
                            {"scheduleType": "DAILY", "dailyDoseCount": 2}
                            """))
                    .andExpect(status().isOk());

            daily(today)
                    .andExpect(jsonPath("$.data.medicines[0].takenDoses").value(contains(1, 2, 3)))
                    .andExpect(jsonPath("$.data.dailyTakenCount").value(2))
                    .andExpect(jsonPath("$.data.dailyTotalCount").value(2));
        }
    }

    @Nested
    @DisplayName("끊은 약")
    class Stopped {

        @Test
        @DisplayName("끊은 약의 기록은 날짜별·기간 조회에 남고, 새 체크·취소는 400")
        void stoppedMedicineLogsRemain() throws Exception {
            long id = registerDaily(pentasaTabletId, 2);
            toggle(myToken, id, today, 1);
            mockMvc.perform(delete(MY + "/" + id).header("Authorization", myToken)).andExpect(status().isOk());

            mockMvc.perform(get(MY).header("Authorization", myToken)).andExpect(jsonPath("$.data", hasSize(0)));
            daily(today)
                    .andExpect(jsonPath("$.data.medicines", hasSize(1)))
                    .andExpect(jsonPath("$.data.medicines[0].userMedicineId").value(id))
                    .andExpect(jsonPath("$.data.medicines[0].stopped").value(true))
                    .andExpect(jsonPath("$.data.medicines[0].medicine.brandName").value("펜타사"))
                    .andExpect(jsonPath("$.data.medicines[0].takenDoses").value(contains(1)))
                    .andExpect(jsonPath("$.data.dailyTotalCount").value(0));
            mockMvc.perform(get(RECORD).param("from", today.toString()).param("to", today.toString()).header("Authorization", myToken))
                    .andExpect(jsonPath("$.data", hasSize(1)))
                    .andExpect(jsonPath("$.data[0].userMedicineId").value(id))
                    .andExpect(jsonPath("$.data[0].medicine.brandName").value("펜타사"));

            toggle(myToken, id, today, 2).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("복용을 중단한 약에는 기록할 수 없습니다."));
            toggle(myToken, id, today, 1).andExpect(status().isBadRequest());
            assertThat(medicationLogRepository.findAll().getFirst().isDeleted()).isFalse();
        }
    }

    @Nested
    @DisplayName("날짜별·기간 조회")
    class Query {

        @Test
        @DisplayName("오늘 복약 n/m은 DAILY만 세고, INTERVAL은 마지막 투여일과 다음 예정일을 준다")
        void dailySummary() throws Exception {
            long pentasa = registerDaily(pentasaTabletId, 3);
            long humira = registerInterval(humiraId, 14);
            long loperamide = idOf(register(myToken, """
                    {"medicineId": %d, "scheduleType": "AS_NEEDED"}
                    """.formatted(loperamideId)));
            toggle(myToken, pentasa, today, 1);
            toggle(myToken, humira, today, 1);

            daily(today)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.date").value(today.toString()))
                    .andExpect(jsonPath("$.data.dailyTakenCount").value(1))
                    .andExpect(jsonPath("$.data.dailyTotalCount").value(3))
                    .andExpect(jsonPath("$.data.medicines[*].userMedicineId").value(contains((int) pentasa, (int) humira, (int) loperamide)))
                    .andExpect(jsonPath("$.data.medicines[1].lastTakenDate").value(today.toString()))
                    .andExpect(jsonPath("$.data.medicines[1].nextDueDate").value(today.plusDays(14).toString()))
                    .andExpect(jsonPath("$.data.medicines[2].takenDoses", hasSize(0)))
                    .andExpect(jsonPath("$.data.medicines[2].nextDueDate").value(nullValue()));
        }

        @Test
        @DisplayName("오늘 등록한 약은 어제 화면에 나오지 않는다")
        void notShownBeforeRegistration() throws Exception {
            registerDaily(pentasaTabletId, 3);

            daily(today.minusDays(1))
                    .andExpect(jsonPath("$.data.medicines", hasSize(0)))
                    .andExpect(jsonPath("$.data.dailyTotalCount").value(0));
        }

        @Test
        @DisplayName("남의 기록은 보이지 않는다")
        void othersHidden() throws Exception {
            long id = registerDaily(pentasaTabletId, 1);
            toggle(myToken, id, today, 1);

            mockMvc.perform(get(RECORD + "/daily").param("date", today.toString()).header("Authorization", otherToken))
                    .andExpect(jsonPath("$.data.medicines", hasSize(0)));
            mockMvc.perform(get(RECORD).param("from", today.toString()).param("to", today.toString()).header("Authorization", otherToken))
                    .andExpect(jsonPath("$.data", hasSize(0)));
        }

        @Test
        @DisplayName("기간 조회 - from이 to보다 늦으면 400, 파라미터가 없으면 400")
        void periodInvalid() throws Exception {
            mockMvc.perform(get(RECORD).param("from", today.toString()).param("to", today.minusDays(1).toString())
                            .header("Authorization", myToken))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("날짜 범위가 올바르지 않습니다."));
            mockMvc.perform(get(RECORD).header("Authorization", myToken)).andExpect(status().isBadRequest());
            mockMvc.perform(get(RECORD + "/daily").header("Authorization", myToken)).andExpect(status().isBadRequest());
        }
    }
}
