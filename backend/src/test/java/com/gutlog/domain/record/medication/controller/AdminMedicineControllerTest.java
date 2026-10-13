// [권한] 직접 작성 연습용. '// [권한]' 주석은 권한 기능용 테스트다. 권한 코드를 다시 작성한 뒤 주석을 풀어 확인한다.
// [권한] 관리자 API 통합 테스트 전체. 일반 사용자 403, 토큰 없음 401, 회원가입으로 ADMIN 불가, 비활성 약 동작 등을 확인한다.
// [권한] package com.gutlog.domain.record.medication.controller;
// [권한]
// [권한] import com.gutlog.domain.record.medication.repository.MedicationLogRepository;
// [권한] import com.gutlog.domain.record.medication.repository.UserMedicineRepository;
// [권한] import com.gutlog.domain.user.repository.UserRepository;
// [권한] import com.jayway.jsonpath.JsonPath;
// [권한] import org.junit.jupiter.api.AfterEach;
// [권한] import org.junit.jupiter.api.BeforeEach;
// [권한] import org.junit.jupiter.api.DisplayName;
// [권한] import org.junit.jupiter.api.Nested;
// [권한] import org.junit.jupiter.api.Test;
// [권한] import org.springframework.beans.factory.annotation.Autowired;
// [권한] import org.springframework.boot.test.context.SpringBootTest;
// [권한] import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
// [권한] import org.springframework.http.MediaType;
// [권한] import org.springframework.jdbc.core.JdbcTemplate;
// [권한] import org.springframework.test.web.servlet.MockMvc;
// [권한] import org.springframework.test.web.servlet.ResultActions;
// [권한]
// [권한] import java.time.Clock;
// [권한] import java.time.LocalDate;
// [권한]
// [권한] import static org.hamcrest.Matchers.greaterThanOrEqualTo;
// [권한] import static org.hamcrest.Matchers.hasItem;
// [권한] import static org.hamcrest.Matchers.hasSize;
// [권한] import static org.hamcrest.Matchers.not;
// [권한] import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
// [권한] import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
// [권한] import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
// [권한]
// [권한] /**
// [권한]  * 관리자 약 관리 + 권한 통합 테스트.
// [권한]  * 서버 시작 시 들어간 초기 약 목록(33건)은 건드리지 않고, 테스트에서 만든 약은 "테스트" 성분명으로 만들어 끝날 때 지운다.
// [권한]  * 관리자는 회원가입으로 만들 수 없으므로 DB에서 직접 권한을 바꾼 뒤 로그인한다.
// [권한]  */
// [권한] @SpringBootTest
// [권한] @AutoConfigureMockMvc
// [권한] class AdminMedicineControllerTest {
// [권한]
// [권한]     private static final String ADMIN_BASE = "/api/admin/medicines";
// [권한]
// [권한]     @Autowired MockMvc mockMvc;
// [권한]     @Autowired JdbcTemplate jdbcTemplate;
// [권한]     @Autowired UserRepository userRepository;
// [권한]     @Autowired UserMedicineRepository userMedicineRepository;
// [권한]     @Autowired MedicationLogRepository medicationLogRepository;
// [권한]     @Autowired Clock clock;
// [권한]
// [권한]     private String userToken;
// [권한]     private String adminToken;
// [권한]
// [권한]     @BeforeEach
// [권한]     void setUp() throws Exception {
// [권한]         signup("normaluser");
// [권한]         signup("adminuser");
// [권한]         jdbcTemplate.update("update users set authority = 'ADMIN' where username = ?", "adminuser");
// [권한]         userToken = login("normaluser");
// [권한]         adminToken = login("adminuser");
// [권한]     }
// [권한]
// [권한]     @AfterEach
// [권한]     void cleanUp() {
// [권한]         medicationLogRepository.deleteAllInBatch();
// [권한]         userMedicineRepository.deleteAllInBatch();
// [권한]         jdbcTemplate.update("delete from medicine where generic_name like '테스트%'");
// [권한]         userRepository.deleteAllInBatch();
// [권한]     }
// [권한]
// [권한]     private void signup(String username) throws Exception {
// [권한]         mockMvc.perform(post("/api/users/signup").contentType(MediaType.APPLICATION_JSON).content("""
// [권한]                         {"username": "%s", "password": "password123", "nickname": "%s", "role": "GUARDIAN"}
// [권한]                         """.formatted(username, username.substring(0, 6))))
// [권한]                 .andExpect(status().isOk());
// [권한]     }
// [권한]
// [권한]     private String login(String username) throws Exception {
// [권한]         String body = mockMvc.perform(post("/api/users/login").contentType(MediaType.APPLICATION_JSON).content("""
// [권한]                         {"username": "%s", "password": "password123"}
// [권한]                         """.formatted(username)))
// [권한]                 .andExpect(status().isOk())
// [권한]                 .andReturn().getResponse().getContentAsString();
// [권한]         return "Bearer " + JsonPath.read(body, "$.data.accessToken");
// [권한]     }
// [권한]
// [권한]     private static String medicineJson(String generic, String brand, String form, String category) {
// [권한]         return """
// [권한]                 {"genericName": "%s", "brandName": "%s", "dosageForm": "%s", "category": "%s"}
// [권한]                 """.formatted(generic, brand, form, category);
// [권한]     }
// [권한]
// [권한]     private ResultActions createMedicine(String token, String json) throws Exception {
// [권한]         return mockMvc.perform(post(ADMIN_BASE).header("Authorization", token).contentType(MediaType.APPLICATION_JSON).content(json));
// [권한]     }
// [권한]
// [권한]     private long createMedicineId(String json) throws Exception {
// [권한]         String body = createMedicine(adminToken, json).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
// [권한]         return ((Number) JsonPath.read(body, "$.data.id")).longValue();
// [권한]     }
// [권한]
// [권한]     @Nested
// [권한]     @DisplayName("권한")
// [권한]     class Authorization {
// [권한]
// [권한]         @Test
// [권한]         @DisplayName("일반 사용자가 관리자 API에 접근하면 403")
// [권한]         void userForbidden() throws Exception {
// [권한]             mockMvc.perform(get(ADMIN_BASE).header("Authorization", userToken)).andExpect(status().isForbidden());
// [권한]             createMedicine(userToken, medicineJson("테스트성분", "테스트약", "TABLET", "OTHER")).andExpect(status().isForbidden());
// [권한]             mockMvc.perform(patch(ADMIN_BASE + "/1/deactivate").header("Authorization", userToken)).andExpect(status().isForbidden());
// [권한]         }
// [권한]
// [권한]         @Test
// [권한]         @DisplayName("토큰 없이 접근하면 401")
// [권한]         void anonymousUnauthorized() throws Exception {
// [권한]             mockMvc.perform(get(ADMIN_BASE)).andExpect(status().isUnauthorized());
// [권한]         }
// [권한]
// [권한]         @Test
// [권한]         @DisplayName("로그인 응답의 유저 정보에 권한이 들어 있고, 회원가입은 항상 USER")
// [권한]         void authorityInUserInfo() throws Exception {
// [권한]             mockMvc.perform(get("/api/users/me").header("Authorization", userToken))
// [권한]                     .andExpect(jsonPath("$.data.authority").value("USER"));
// [권한]             mockMvc.perform(get("/api/users/me").header("Authorization", adminToken))
// [권한]                     .andExpect(jsonPath("$.data.authority").value("ADMIN"));
// [권한]         }
// [권한]
// [권한]         @Test
// [권한]         @DisplayName("회원가입 요청에 authority를 넣어도 ADMIN이 되지 않는다")
// [권한]         void cannotSignupAsAdmin() throws Exception {
// [권한]             mockMvc.perform(post("/api/users/signup").contentType(MediaType.APPLICATION_JSON).content("""
// [권한]                             {"username": "sneaky1", "password": "password123", "nickname": "몰래", "role": "OTHER", "authority": "ADMIN"}
// [권한]                             """))
// [권한]                     .andExpect(status().isOk())
// [권한]                     .andExpect(jsonPath("$.data.authority").value("USER"));
// [권한]             mockMvc.perform(get(ADMIN_BASE).header("Authorization", login("sneaky1"))).andExpect(status().isForbidden());
// [권한]         }
// [권한]     }
// [권한]
// [권한]     @Nested
// [권한]     @DisplayName("관리자 약 관리")
// [권한]     class Manage {
// [권한]
// [권한]         @Test
// [권한]         @DisplayName("목록 - 초기 약 목록이 들어 있고, 비활성 약도 포함한다")
// [권한]         void listIncludesInactive() throws Exception {
// [권한]             long id = createMedicineId(medicineJson("테스트성분", "테스트약", "TABLET", "OTHER"));
// [권한]             mockMvc.perform(patch(ADMIN_BASE + "/" + id + "/deactivate").header("Authorization", adminToken)).andExpect(status().isOk());
// [권한]
// [권한]             mockMvc.perform(get(ADMIN_BASE).header("Authorization", adminToken))
// [권한]                     .andExpect(status().isOk())
// [권한]                     .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(34))))
// [권한]                     .andExpect(jsonPath("$.data[?(@.id == %d)].active".formatted(id)).value(false));
// [권한]         }
// [권한]
// [권한]         @Test
// [권한]         @DisplayName("추가 - 제형에 맞는 복용 단위가 응답에 온다. 같은 약·제형을 또 추가하면 409")
// [권한]         void createAndDuplicate() throws Exception {
// [권한]             createMedicine(adminToken, medicineJson("테스트성분", "테스트약", "ENEMA", "AMINOSALICYLATE"))
// [권한]                     .andExpect(status().isOk())
// [권한]                     .andExpect(jsonPath("$.data.doseUnit").value("회"))
// [권한]                     .andExpect(jsonPath("$.data.active").value(true));
// [권한]
// [권한]             createMedicine(adminToken, medicineJson("테스트성분", "테스트약", "ENEMA", "AMINOSALICYLATE"))
// [권한]                     .andExpect(status().isConflict())
// [권한]                     .andExpect(jsonPath("$.message").value("이미 등록된 약입니다."));
// [권한]             // 제형이 다르면 다른 약
// [권한]             createMedicine(adminToken, medicineJson("테스트성분", "테스트약", "SUPPOSITORY", "AMINOSALICYLATE"))
// [권한]                     .andExpect(status().isOk());
// [권한]         }
// [권한]
// [권한]         @Test
// [권한]         @DisplayName("추가 실패 - 검증 메시지, 없는 제형 값")
// [권한]         void createInvalid() throws Exception {
// [권한]             createMedicine(adminToken, medicineJson(" ", "테스트약", "TABLET", "OTHER"))
// [권한]                     .andExpect(status().isBadRequest())
// [권한]                     .andExpect(jsonPath("$.message").value("성분명은 필수입니다."));
// [권한]             createMedicine(adminToken, medicineJson("테스트성분", "테스트약", "PILL", "OTHER"))
// [권한]                     .andExpect(status().isBadRequest());
// [권한]         }
// [권한]
// [권한]         @Test
// [권한]         @DisplayName("수정 - 이름·제형·분류를 바꾼다. 없는 약이면 404")
// [권한]         void edit() throws Exception {
// [권한]             long id = createMedicineId(medicineJson("테스트성분", "테스트약", "TABLET", "OTHER"));
// [권한]
// [권한]             mockMvc.perform(put(ADMIN_BASE + "/" + id).header("Authorization", adminToken)
// [권한]                             .contentType(MediaType.APPLICATION_JSON)
// [권한]                             .content(medicineJson("테스트성분2", "테스트약2", "CAPSULE", "BIOLOGIC")))
// [권한]                     .andExpect(status().isOk())
// [권한]                     .andExpect(jsonPath("$.data.genericName").value("테스트성분2"))
// [권한]                     .andExpect(jsonPath("$.data.doseUnit").value("캡슐"))
// [권한]                     .andExpect(jsonPath("$.data.category").value("BIOLOGIC"));
// [권한]
// [권한]             mockMvc.perform(put(ADMIN_BASE + "/999999").header("Authorization", adminToken)
// [권한]                             .contentType(MediaType.APPLICATION_JSON)
// [권한]                             .content(medicineJson("테스트성분3", "테스트약3", "TABLET", "OTHER")))
// [권한]                     .andExpect(status().isNotFound());
// [권한]         }
// [권한]
// [권한]         @Test
// [권한]         @DisplayName("비활성화하면 검색·새 등록에서 빠지지만, 이미 복용 중인 사용자는 목록에서 보고 계속 체크한다. 재활성화하면 다시 검색된다")
// [권한]         void deactivateKeepsExistingUsers() throws Exception {
// [권한]             long medicineId = createMedicineId(medicineJson("테스트성분", "테스트비활성약", "TABLET", "OTHER"));
// [권한]             String register = """
// [권한]                     {"medicineId": %d, "scheduleType": "DAILY", "dailyDoseCount": 1}
// [권한]                     """.formatted(medicineId);
// [권한]             String body = mockMvc.perform(post("/api/my-medicines").header("Authorization", userToken)
// [권한]                             .contentType(MediaType.APPLICATION_JSON).content(register))
// [권한]                     .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
// [권한]             long userMedicineId = ((Number) JsonPath.read(body, "$.data.id")).longValue();
// [권한]
// [권한]             mockMvc.perform(patch(ADMIN_BASE + "/" + medicineId + "/deactivate").header("Authorization", adminToken))
// [권한]                     .andExpect(status().isOk())
// [권한]                     .andExpect(jsonPath("$.data.active").value(false));
// [권한]
// [권한]             // 검색에서 빠진다
// [권한]             mockMvc.perform(get("/api/medicines").param("keyword", "테스트비활성").header("Authorization", userToken))
// [권한]                     .andExpect(jsonPath("$.data", hasSize(0)));
// [권한]             mockMvc.perform(get("/api/medicines").header("Authorization", userToken))
// [권한]                     .andExpect(jsonPath("$.data[*].id", not(hasItem((int) medicineId))));
// [권한]             // 이미 복용 중인 사용자에게는 이름이 그대로 보이고, 계속 체크할 수 있다
// [권한]             mockMvc.perform(get("/api/my-medicines").header("Authorization", userToken))
// [권한]                     .andExpect(jsonPath("$.data[0].medicine.brandName").value("테스트비활성약"))
// [권한]                     .andExpect(jsonPath("$.data[0].medicine.active").value(false));
// [권한]             mockMvc.perform(post("/api/record/medication/%d/%s/1/toggle".formatted(userMedicineId, LocalDate.now(clock)))
// [권한]                             .header("Authorization", userToken))
// [권한]                     .andExpect(status().isOk())
// [권한]                     .andExpect(jsonPath("$.data.taken").value(true));
// [권한]             // 새로 등록할 수는 없다 (관리자 계정으로 등록 시도)
// [권한]             mockMvc.perform(post("/api/my-medicines").header("Authorization", adminToken)
// [권한]                             .contentType(MediaType.APPLICATION_JSON).content(register))
// [권한]                     .andExpect(status().isNotFound())
// [권한]                     .andExpect(jsonPath("$.message").value("약을 찾을 수 없습니다."));
// [권한]
// [권한]             mockMvc.perform(patch(ADMIN_BASE + "/" + medicineId + "/activate").header("Authorization", adminToken))
// [권한]                     .andExpect(jsonPath("$.data.active").value(true));
// [권한]             mockMvc.perform(get("/api/medicines").param("keyword", "테스트비활성").header("Authorization", userToken))
// [권한]                     .andExpect(jsonPath("$.data", hasSize(1)));
// [권한]         }
// [권한]     }
// [권한] }
