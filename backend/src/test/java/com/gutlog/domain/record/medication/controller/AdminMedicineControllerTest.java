package com.gutlog.domain.record.medication.controller;

import com.gutlog.domain.record.medication.repository.MedicationLogRepository;
import com.gutlog.domain.record.medication.repository.MedicationScheduleRepository;
import com.gutlog.domain.record.medication.repository.UserMedicineRepository;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Clock;
import java.time.LocalDate;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 관리자 약 관리 + 권한 통합 테스트.
 * 서버 시작 시 들어간 초기 약 목록(33건)은 건드리지 않고, 테스트에서 만든 약은 "테스트" 성분명으로 만들어 끝날 때 지운다.
 * 관리자는 회원가입으로 만들 수 없으므로 DB에서 직접 권한을 바꾼 뒤 로그인한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AdminMedicineControllerTest {

    private static final String ADMIN_BASE = "/api/admin/medicines";

    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired UserRepository userRepository;
    @Autowired UserMedicineRepository userMedicineRepository;
    @Autowired MedicationLogRepository medicationLogRepository;
    @Autowired MedicationScheduleRepository medicationScheduleRepository;
    @Autowired Clock clock;

    private String userToken;
    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        signup("normaluser");
        signup("adminuser");
        jdbcTemplate.update("update users set authority = 'ADMIN' where username = ?", "adminuser");
        userToken = login("normaluser");
        adminToken = login("adminuser");
    }

    @AfterEach
    void cleanUp() {
        medicationLogRepository.deleteAllInBatch();
        medicationScheduleRepository.deleteAllInBatch();   // 일정이 복용 약을 가리키므로 먼저 지운다
        userMedicineRepository.deleteAllInBatch();
        jdbcTemplate.update("delete from medicine where generic_name like '테스트%'");
        userRepository.deleteAllInBatch();
    }

    private void signup(String username) throws Exception {
        mockMvc.perform(post("/api/users/signup").contentType(MediaType.APPLICATION_JSON).content("""
                        {"username": "%s", "password": "password123", "nickname": "%s", "role": "GUARDIAN"}
                        """.formatted(username, username.substring(0, 6))))
                .andExpect(status().isOk());
    }

    private String login(String username) throws Exception {
        String body = mockMvc.perform(post("/api/users/login").contentType(MediaType.APPLICATION_JSON).content("""
                        {"username": "%s", "password": "password123"}
                        """.formatted(username)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(body, "$.data.accessToken");
    }

    private static String medicineJson(String generic, String brand, String form, String category) {
        return """
                {"genericName": "%s", "brandName": "%s", "dosageForm": "%s", "category": "%s"}
                """.formatted(generic, brand, form, category);
    }

    private ResultActions createMedicine(String token, String json) throws Exception {
        return mockMvc.perform(post(ADMIN_BASE).header("Authorization", token).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private long createMedicineId(String json) throws Exception {
        String body = createMedicine(adminToken, json).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.data.id")).longValue();
    }

    @Nested
    @DisplayName("권한")
    class Authorization {

        @Test
        @DisplayName("일반 사용자가 관리자 API에 접근하면 403")
        void userForbidden() throws Exception {
            mockMvc.perform(get(ADMIN_BASE).header("Authorization", userToken)).andExpect(status().isForbidden());
            createMedicine(userToken, medicineJson("테스트성분", "테스트약", "TABLET", "OTHER")).andExpect(status().isForbidden());
            mockMvc.perform(patch(ADMIN_BASE + "/1/deactivate").header("Authorization", userToken)).andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("토큰 없이 접근하면 401")
        void anonymousUnauthorized() throws Exception {
            mockMvc.perform(get(ADMIN_BASE)).andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("로그인 응답의 유저 정보에 권한이 들어 있고, 회원가입은 항상 USER")
        void authorityInUserInfo() throws Exception {
            mockMvc.perform(get("/api/users/me").header("Authorization", userToken))
                    .andExpect(jsonPath("$.data.authority").value("USER"));
            mockMvc.perform(get("/api/users/me").header("Authorization", adminToken))
                    .andExpect(jsonPath("$.data.authority").value("ADMIN"));
        }

        @Test
        @DisplayName("회원가입 요청에 authority를 넣어도 ADMIN이 되지 않는다")
        void cannotSignupAsAdmin() throws Exception {
            mockMvc.perform(post("/api/users/signup").contentType(MediaType.APPLICATION_JSON).content("""
                            {"username": "sneaky1", "password": "password123", "nickname": "몰래", "role": "OTHER", "authority": "ADMIN"}
                            """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.authority").value("USER"));
            mockMvc.perform(get(ADMIN_BASE).header("Authorization", login("sneaky1"))).andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("관리자 약 관리")
    class Manage {

        @Test
        @DisplayName("목록 - 초기 약 목록이 들어 있고, 비활성 약도 포함한다")
        void listIncludesInactive() throws Exception {
            long id = createMedicineId(medicineJson("테스트성분", "테스트약", "TABLET", "OTHER"));
            mockMvc.perform(patch(ADMIN_BASE + "/" + id + "/deactivate").header("Authorization", adminToken)).andExpect(status().isOk());

            mockMvc.perform(get(ADMIN_BASE).header("Authorization", adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(34))))
                    .andExpect(jsonPath("$.data[?(@.id == %d)].active".formatted(id)).value(false));
        }

        @Test
        @DisplayName("추가 - 제형에 맞는 복용 단위가 응답에 온다. 같은 약·제형을 또 추가하면 409")
        void createAndDuplicate() throws Exception {
            createMedicine(adminToken, medicineJson("테스트성분", "테스트약", "ENEMA", "AMINOSALICYLATE"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.doseUnit").value("회"))
                    .andExpect(jsonPath("$.data.active").value(true));

            createMedicine(adminToken, medicineJson("테스트성분", "테스트약", "ENEMA", "AMINOSALICYLATE"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value("이미 등록된 약입니다."));
            // 제형이 다르면 다른 약
            createMedicine(adminToken, medicineJson("테스트성분", "테스트약", "SUPPOSITORY", "AMINOSALICYLATE"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("추가 실패 - 검증 메시지, 없는 제형 값")
        void createInvalid() throws Exception {
            createMedicine(adminToken, medicineJson(" ", "테스트약", "TABLET", "OTHER"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("성분명은 필수입니다."));
            createMedicine(adminToken, medicineJson("테스트성분", "테스트약", "PILL", "OTHER"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("수정 - 이름·제형·분류를 바꾼다. 없는 약이면 404")
        void edit() throws Exception {
            long id = createMedicineId(medicineJson("테스트성분", "테스트약", "TABLET", "OTHER"));

            mockMvc.perform(put(ADMIN_BASE + "/" + id).header("Authorization", adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(medicineJson("테스트성분2", "테스트약2", "CAPSULE", "BIOLOGIC")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.genericName").value("테스트성분2"))
                    .andExpect(jsonPath("$.data.doseUnit").value("캡슐"))
                    .andExpect(jsonPath("$.data.category").value("BIOLOGIC"));

            mockMvc.perform(put(ADMIN_BASE + "/999999").header("Authorization", adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(medicineJson("테스트성분3", "테스트약3", "TABLET", "OTHER")))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("비활성화하면 검색·새 등록에서 빠지지만, 이미 복용 중인 사용자는 목록에서 보고 계속 체크한다. 재활성화하면 다시 검색된다")
        void deactivateKeepsExistingUsers() throws Exception {
            long medicineId = createMedicineId(medicineJson("테스트성분", "테스트비활성약", "TABLET", "OTHER"));
            String register = """
                    {"medicineId": %d, "scheduleType": "DAILY", "dailyDoseCount": 1}
                    """.formatted(medicineId);
            String body = mockMvc.perform(post("/api/my-medicines").header("Authorization", userToken)
                            .contentType(MediaType.APPLICATION_JSON).content(register))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            long userMedicineId = ((Number) JsonPath.read(body, "$.data.id")).longValue();

            mockMvc.perform(patch(ADMIN_BASE + "/" + medicineId + "/deactivate").header("Authorization", adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.active").value(false));

            // 검색에서 빠진다
            mockMvc.perform(get("/api/medicines").param("keyword", "테스트비활성").header("Authorization", userToken))
                    .andExpect(jsonPath("$.data", hasSize(0)));
            mockMvc.perform(get("/api/medicines").header("Authorization", userToken))
                    .andExpect(jsonPath("$.data[*].id", not(hasItem((int) medicineId))));
            // 이미 복용 중인 사용자에게는 이름이 그대로 보이고, 계속 체크할 수 있다
            mockMvc.perform(get("/api/my-medicines").header("Authorization", userToken))
                    .andExpect(jsonPath("$.data[0].medicine.brandName").value("테스트비활성약"))
                    .andExpect(jsonPath("$.data[0].medicine.active").value(false));
            mockMvc.perform(post("/api/record/medication/%d/%s/1/toggle".formatted(userMedicineId, LocalDate.now(clock)))
                            .header("Authorization", userToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.taken").value(true));
            // 새로 등록할 수는 없다 (관리자 계정으로 등록 시도)
            mockMvc.perform(post("/api/my-medicines").header("Authorization", adminToken)
                            .contentType(MediaType.APPLICATION_JSON).content(register))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("약을 찾을 수 없습니다."));

            mockMvc.perform(patch(ADMIN_BASE + "/" + medicineId + "/activate").header("Authorization", adminToken))
                    .andExpect(jsonPath("$.data.active").value(true));
            mockMvc.perform(get("/api/medicines").param("keyword", "테스트비활성").header("Authorization", userToken))
                    .andExpect(jsonPath("$.data", hasSize(1)));
        }
    }
}
