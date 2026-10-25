package com.gutlog.domain.record.medication.controller;

import com.gutlog.domain.record.medication.entity.DosageForm;
import com.gutlog.domain.record.medication.repository.MedicationLogRepository;
import com.gutlog.domain.record.medication.repository.MedicationScheduleRepository;
import com.gutlog.domain.record.medication.repository.MedicineRepository;
import com.gutlog.domain.record.medication.repository.UserMedicineRepository;
import com.gutlog.domain.user.repository.UserRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 복용 일정 이력 통합 테스트. 날짜를 옮겨 가며 "수정·중단해도 지난 날짜는 그때 일정대로 남는지"를 HTTP로 확인한다.
 * 서버의 Clock을 바꿀 수 있는 시계로 대신한다. (등록·수정·중단 날짜, 미래 날짜 판단이 모두 이 시계를 따른다)
 *
 * 시나리오: 10/1 펜타사 하루 3회로 등록 → 10/5 하루 2회로 수정 → 10/7 복용 중단
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(MedicationScheduleHistoryTest.MutableClockConfig.class)
class MedicationScheduleHistoryTest {

    private static final String MY = "/api/my-medicines";
    private static final String RECORD = "/api/record/medication";
    private static final LocalDate REGISTERED = LocalDate.of(2026, 10, 1);
    private static final LocalDate CHANGED = LocalDate.of(2026, 10, 5);
    private static final LocalDate STOPPED = LocalDate.of(2026, 10, 7);

    @Autowired MockMvc mockMvc;
    @Autowired MutableClock clock;
    @Autowired UserRepository userRepository;
    @Autowired MedicineRepository medicineRepository;
    @Autowired UserMedicineRepository userMedicineRepository;
    @Autowired MedicationScheduleRepository medicationScheduleRepository;
    @Autowired MedicationLogRepository medicationLogRepository;

    private String token;
    private long pentasaTabletId;

    @TestConfiguration
    static class MutableClockConfig {
        @Bean
        @Primary
        MutableClock mutableClock() {
            return new MutableClock();
        }
    }

    /** 원하는 날짜(서울 12:00)로 옮길 수 있는 시계 */
    static class MutableClock extends Clock {
        private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
        private Instant instant = Instant.now();

        void setDate(LocalDate date) {
            this.instant = date.atTime(12, 0).atZone(SEOUL).toInstant();
        }

        @Override
        public ZoneId getZone() {
            return SEOUL;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }

    @BeforeEach
    void setUp() throws Exception {
        clock.setDate(REGISTERED);
        token = signupAndLogin("historyuser");
        pentasaTabletId = medicineRepository.findByGenericNameAndBrandNameAndDosageForm("메살라진", "펜타사", DosageForm.TABLET)
                .orElseThrow().getId();
    }

    @AfterEach
    void cleanUp() {
        medicationLogRepository.deleteAllInBatch();
        medicationScheduleRepository.deleteAllInBatch();
        userMedicineRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
    }

    private String signupAndLogin(String username) throws Exception {
        mockMvc.perform(post("/api/users/signup").contentType(MediaType.APPLICATION_JSON).content("""
                        {"username": "%s", "password": "password123", "nickname": "이력", "role": "PATIENT", "ibdType": "CROHNS_DISEASE"}
                        """.formatted(username)))
                .andExpect(status().isOk());
        String body = mockMvc.perform(post("/api/users/login").contentType(MediaType.APPLICATION_JSON).content("""
                        {"username": "%s", "password": "password123"}
                        """.formatted(username)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(body, "$.data.accessToken");
    }

    private long registerDaily(int count) throws Exception {
        String body = mockMvc.perform(post(MY).header("Authorization", token).contentType(MediaType.APPLICATION_JSON).content("""
                        {"medicineId": %d, "scheduleType": "DAILY", "dailyDoseCount": %d}
                        """.formatted(pentasaTabletId, count)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.data.id")).longValue();
    }

    private ResultActions editDaily(long id, int count) throws Exception {
        return mockMvc.perform(put(MY + "/" + id).header("Authorization", token).contentType(MediaType.APPLICATION_JSON).content("""
                {"scheduleType": "DAILY", "dailyDoseCount": %d}
                """.formatted(count)));
    }

    private ResultActions toggle(long id, LocalDate date, int dose) throws Exception {
        return mockMvc.perform(post(RECORD + "/%d/%s/%d/toggle".formatted(id, date, dose)).header("Authorization", token));
    }

    private ResultActions daily(LocalDate date) throws Exception {
        return mockMvc.perform(get(RECORD + "/daily").param("date", date.toString()).header("Authorization", token));
    }

    @Test
    @DisplayName("수정하면 오늘부터 새 일정이 적용되고, 지난 날짜는 그때 일정(하루 3회)과 기록이 그대로 보인다")
    void editKeepsPastSchedule() throws Exception {
        long id = registerDaily(3);
        toggle(id, REGISTERED, 1).andExpect(status().isOk());
        toggle(id, REGISTERED, 2).andExpect(status().isOk());

        clock.setDate(CHANGED);
        editDaily(id, 2).andExpect(status().isOk()).andExpect(jsonPath("$.data.dailyDoseCount").value(2));

        daily(REGISTERED)
                .andExpect(jsonPath("$.data.medicines[0].dailyDoseCount").value(3))
                .andExpect(jsonPath("$.data.medicines[0].takenDoses").value(contains(1, 2)))
                .andExpect(jsonPath("$.data.dailyTakenCount").value(2))
                .andExpect(jsonPath("$.data.dailyTotalCount").value(3));
        daily(CHANGED.minusDays(1)).andExpect(jsonPath("$.data.medicines[0].dailyDoseCount").value(3));
        daily(CHANGED)
                .andExpect(jsonPath("$.data.medicines[0].dailyDoseCount").value(2))
                .andExpect(jsonPath("$.data.dailyTotalCount").value(2));
        mockMvc.perform(get(MY).header("Authorization", token))
                .andExpect(jsonPath("$.data[0].dailyDoseCount").value(2));
        assertThat(medicationScheduleRepository.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("지난 날짜는 그때 일정으로 회차 상한을 검사한다 (수정 후에도 그날은 3회차까지 체크된다)")
    void pastDateUsesPastDoseLimit() throws Exception {
        long id = registerDaily(3);
        clock.setDate(CHANGED);
        editDaily(id, 2).andExpect(status().isOk());

        toggle(id, REGISTERED, 1).andExpect(status().isOk());
        toggle(id, REGISTERED, 2).andExpect(status().isOk());
        toggle(id, REGISTERED, 3).andExpect(status().isOk()).andExpect(jsonPath("$.data.taken").value(true));

        toggle(id, CHANGED, 1).andExpect(status().isOk());
        toggle(id, CHANGED, 2).andExpect(status().isOk());
        toggle(id, CHANGED, 3).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("복용 회차가 올바르지 않습니다."));
    }

    @Test
    @DisplayName("같은 날 여러 번 수정하면 일정이 늘어나지 않고 오늘 일정만 고쳐진다")
    void sameDayEditsOverwrite() throws Exception {
        long id = registerDaily(3);
        clock.setDate(CHANGED);
        editDaily(id, 2).andExpect(status().isOk());
        editDaily(id, 1).andExpect(status().isOk());

        assertThat(medicationScheduleRepository.count()).isEqualTo(2);   // 10/1 일정 + 10/5 일정
        daily(CHANGED).andExpect(jsonPath("$.data.medicines[0].dailyDoseCount").value(1));
        daily(CHANGED.minusDays(1)).andExpect(jsonPath("$.data.medicines[0].dailyDoseCount").value(3));
    }

    @Test
    @DisplayName("등록 전 날짜는 화면에 없고, 체크하면 400")
    void beforeRegistration() throws Exception {
        long id = registerDaily(3);

        daily(REGISTERED.minusDays(1)).andExpect(jsonPath("$.data.medicines", hasSize(0)));
        toggle(id, REGISTERED.minusDays(1), 1).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("그 날짜에는 복용 중인 약이 아닙니다."));
        assertThat(medicationLogRepository.count()).isZero();
    }

    @Test
    @DisplayName("중단하면 그날부터 화면에서 빠지고, 중단 전 날짜는 그때 일정으로 남는다. 중단 후에는 지난 날짜도 체크할 수 없다")
    void stopKeepsPastDays() throws Exception {
        long id = registerDaily(3);
        clock.setDate(CHANGED);
        editDaily(id, 2).andExpect(status().isOk());

        clock.setDate(STOPPED);
        mockMvc.perform(delete(MY + "/" + id).header("Authorization", token)).andExpect(status().isOk());

        daily(STOPPED).andExpect(jsonPath("$.data.medicines", hasSize(0)));
        daily(STOPPED.minusDays(1))
                .andExpect(jsonPath("$.data.medicines[0].dailyDoseCount").value(2))
                .andExpect(jsonPath("$.data.medicines[0].stopped").value(true))
                .andExpect(jsonPath("$.data.dailyTotalCount").value(2));
        daily(REGISTERED).andExpect(jsonPath("$.data.medicines[0].dailyDoseCount").value(3));
        mockMvc.perform(get(MY).header("Authorization", token)).andExpect(jsonPath("$.data", hasSize(0)));
        toggle(id, STOPPED.minusDays(1), 1).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("복용을 중단한 약에는 기록할 수 없습니다."));
    }

    @Test
    @DisplayName("중단한 날 체크해 둔 기록은 그날 화면에 그 전 일정 값으로 남는다")
    void stoppedDayWithLogs() throws Exception {
        long id = registerDaily(3);
        clock.setDate(STOPPED);
        toggle(id, STOPPED, 1).andExpect(status().isOk());
        mockMvc.perform(delete(MY + "/" + id).header("Authorization", token)).andExpect(status().isOk());

        daily(STOPPED)
                .andExpect(jsonPath("$.data.medicines", hasSize(1)))
                .andExpect(jsonPath("$.data.medicines[0].stopped").value(true))
                .andExpect(jsonPath("$.data.medicines[0].dailyDoseCount").value(3))
                .andExpect(jsonPath("$.data.medicines[0].takenDoses").value(contains(1)))
                .andExpect(jsonPath("$.data.dailyTotalCount").value(0));
    }
}
