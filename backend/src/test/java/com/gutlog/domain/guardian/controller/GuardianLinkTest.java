package com.gutlog.domain.guardian.controller;

import com.gutlog.domain.guardian.entity.GuardianLink;
import com.gutlog.domain.guardian.entity.GuardianLinkHistory;
import com.gutlog.domain.guardian.repository.GuardianCodeAttemptRepository;
import com.gutlog.domain.guardian.repository.GuardianInviteCodeRepository;
import com.gutlog.domain.guardian.repository.GuardianLinkHistoryRepository;
import com.gutlog.domain.guardian.repository.GuardianLinkRepository;
import com.gutlog.domain.user.entity.User;
import com.gutlog.domain.user.repository.UserRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;
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
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 연결 조회와 해제, 해제 이력 저장 통합 테스트 (G05, design.md 2장·3.1·3.2, Q2·Q3·Q7·Q9·Q15, C5).
 * 서버의 Clock을 바꿀 수 있는 시계로 대신해 연결·해제 시각을 확인한다.
 * 실제 요청 흐름과 같도록 @Transactional을 걸지 않고, 테스트가 끝날 때마다 데이터를 직접 지운다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(GuardianLinkTest.MutableClockConfig.class)
class GuardianLinkTest {

	private static final String MY_GUARDIAN_URL = "/api/my-guardian";
	private static final String PATIENTS_URL = "/api/guardian/patients";
	private static final String INVITE_URL = "/api/my-guardian/invite-code";
	private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 28, 12, 0);

	private static final String MSG_LINK_NOT_FOUND = "연결 정보를 찾을 수 없습니다.";
	private static final String MSG_GUARDIAN_ALREADY_LINKED = "연결된 보호자를 해제한 뒤 코드를 발급해 주세요.";
	private static final String MSG_PATIENT_LIMIT = "환자는 2명까지 연결할 수 있습니다.";

	@Autowired MockMvc mockMvc;
	@Autowired MutableClock clock;
	@Autowired UserRepository userRepository;
	@Autowired GuardianInviteCodeRepository inviteCodeRepository;
	@Autowired GuardianLinkRepository linkRepository;
	@Autowired GuardianLinkHistoryRepository historyRepository;
	@Autowired GuardianCodeAttemptRepository attemptRepository;

	private String patient1Token;
	private String patient2Token;
	private String patient3Token;
	private String guardian1Token;
	private String guardian2Token;
	private String otherToken;

	@TestConfiguration
	static class MutableClockConfig {
		@Bean
		@Primary
		MutableClock mutableClock() {
			return new MutableClock();
		}
	}

	/** 원하는 시각(서울)으로 옮길 수 있는 시계 */
	static class MutableClock extends Clock {
		private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
		private volatile Instant instant = Instant.now();

		void setTime(LocalDateTime time) {
			this.instant = time.atZone(SEOUL).toInstant();
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
		clock.setTime(NOW);
		patient1Token = signupAndLogin("patient1", "환자1", "PATIENT");
		patient2Token = signupAndLogin("patient2", "환자2", "PATIENT");
		patient3Token = signupAndLogin("patient3", "환자3", "PATIENT");
		guardian1Token = signupAndLogin("guardian1", "보호자1", "GUARDIAN");
		guardian2Token = signupAndLogin("guardian2", "보호자2", "GUARDIAN");
		otherToken = signupAndLogin("other1", "기타", "OTHER");
	}

	@AfterEach
	void cleanUp() {
		inviteCodeRepository.deleteAllInBatch();
		linkRepository.deleteAllInBatch();
		historyRepository.deleteAllInBatch();
		attemptRepository.deleteAllInBatch();
		userRepository.deleteAllInBatch();
	}

	private String signupAndLogin(String username, String nickname, String role) throws Exception {
		String ibdType = role.equals("PATIENT") ? ", \"ibdType\": \"ULCERATIVE_COLITIS\"" : "";
		mockMvc.perform(post("/api/users/signup")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"username": "%s", "password": "password123", "nickname": "%s", "role": "%s"%s}
								""".formatted(username, nickname, role, ibdType)))
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

	/** 환자가 실제 API로 코드를 발급받는다. */
	private String issuedCode(String patientToken) throws Exception {
		String body = mockMvc.perform(post(INVITE_URL).header("Authorization", patientToken))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		return JsonPath.read(body, "$.data.code");
	}

	private ResultActions connect(String guardianToken, String code) throws Exception {
		return mockMvc.perform(post(PATIENTS_URL)
				.header("Authorization", guardianToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"code": "%s"}
						""".formatted(code)));
	}

	/** 실제 API로 코드를 발급받아 연결한다. */
	private void link(String patientToken, String guardianToken) throws Exception {
		connect(guardianToken, issuedCode(patientToken)).andExpect(status().isOk());
	}

	private ResultActions getMyGuardian(String token) throws Exception {
		return mockMvc.perform(get(MY_GUARDIAN_URL).header("Authorization", token));
	}

	private ResultActions disconnectMyGuardian(String token) throws Exception {
		return mockMvc.perform(delete(MY_GUARDIAN_URL).header("Authorization", token));
	}

	private ResultActions getPatients(String token) throws Exception {
		return mockMvc.perform(get(PATIENTS_URL).header("Authorization", token));
	}

	private ResultActions disconnectPatient(String token, Long patientId) throws Exception {
		return mockMvc.perform(delete(PATIENTS_URL + "/" + patientId).header("Authorization", token));
	}

	private User user(String username) {
		return userRepository.findByUsername(username).orElseThrow();
	}

	private Long id(String username) {
		return user(username).getId();
	}

	/** 저장된 이력을 저장한 순서로 */
	private List<GuardianLinkHistory> histories() {
		return historyRepository.findAll().stream()
				.sorted(Comparator.comparing(GuardianLinkHistory::getId))
				.toList();
	}

	private void assertHistory(GuardianLinkHistory history, String patient, String guardian,
			LocalDateTime connectedAt, LocalDateTime disconnectedAt, String disconnectedBy) {
		assertThat(history.getPatient().getId()).isEqualTo(id(patient));
		assertThat(history.getGuardian().getId()).isEqualTo(id(guardian));
		assertThat(history.getConnectedAt()).isEqualTo(connectedAt);
		assertThat(history.getDisconnectedAt()).isEqualTo(disconnectedAt);
		assertThat(history.getDisconnectedBy().getId()).isEqualTo(id(disconnectedBy));
	}

	@Nested
	@DisplayName("조회 - 환자와 보호자가 각자 자기 연결을 본다 (3.1, 3.2, Q15, C5)")
	class Lookup {

		@Test
		@DisplayName("환자: 연결이 없으면 data는 null이다")
		void patientNoLink() throws Exception {
			getMyGuardian(patient1Token)
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.success").value(true))
					.andExpect(jsonPath("$.data").value(nullValue()));
		}

		@Test
		@DisplayName("환자: 보호자 닉네임·연결 시각·공개 범위(전부 PRIVATE)를 본다")
		void patientWithLink() throws Exception {
			clock.setTime(NOW.plusMinutes(3));
			link(patient1Token, guardian1Token);
			clock.setTime(NOW.plusDays(1));

			getMyGuardian(patient1Token)
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.data.guardianNickname").value("보호자1"))
					.andExpect(jsonPath("$.data.connectedAt").value("2026-11-28T12:03:00"))
					.andExpect(jsonPath("$.data.access.meal").value("PRIVATE"))
					.andExpect(jsonPath("$.data.access.bowel").value("PRIVATE"))
					.andExpect(jsonPath("$.data.access.medication").value("PRIVATE"))
					.andExpect(jsonPath("$.data.access.weight").value("PRIVATE"))
					.andExpect(jsonPath("$.data.access.sleep").value("PRIVATE"));
		}

		@Test
		@DisplayName("환자: 다른 환자의 연결은 보이지 않는다")
		void patientSeesOnlyOwn() throws Exception {
			link(patient1Token, guardian1Token);

			getMyGuardian(patient2Token)
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.data").value(nullValue()));
		}

		@Test
		@DisplayName("보호자: 연결이 없으면 빈 배열이다")
		void guardianNoLink() throws Exception {
			getPatients(guardian1Token)
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.success").value(true))
					.andExpect(jsonPath("$.data", hasSize(0)));
		}

		@Test
		@DisplayName("보호자: 환자 2명을 먼저 연결한 순서로 본다 (환자 id·닉네임·연결 시각·공개 범위)")
		void guardianTwoPatients() throws Exception {
			clock.setTime(NOW.plusMinutes(1));
			link(patient2Token, guardian1Token);
			clock.setTime(NOW.plusMinutes(5));
			link(patient1Token, guardian1Token);

			getPatients(guardian1Token)
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.data", hasSize(2)))
					.andExpect(jsonPath("$.data[0].patientId").value(id("patient2")))
					.andExpect(jsonPath("$.data[0].nickname").value("환자2"))
					.andExpect(jsonPath("$.data[0].connectedAt").value("2026-11-28T12:01:00"))
					.andExpect(jsonPath("$.data[0].access.meal").value("PRIVATE"))
					.andExpect(jsonPath("$.data[0].access.sleep").value("PRIVATE"))
					.andExpect(jsonPath("$.data[1].patientId").value(id("patient1")))
					.andExpect(jsonPath("$.data[1].nickname").value("환자1"))
					.andExpect(jsonPath("$.data[1].connectedAt").value("2026-11-28T12:05:00"));
		}

		@Test
		@DisplayName("보호자: 다른 보호자의 환자는 보이지 않는다")
		void guardianSeesOnlyOwn() throws Exception {
			link(patient1Token, guardian1Token);
			link(patient2Token, guardian2Token);

			getPatients(guardian2Token)
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.data", hasSize(1)))
					.andExpect(jsonPath("$.data[0].patientId").value(id("patient2")));
		}
	}

	@Nested
	@DisplayName("역할 확인 - 맞지 않는 역할은 404, 토큰이 없으면 401")
	class RoleCheck {

		@Test
		@DisplayName("GUARDIAN·OTHER가 환자 연결 조회를 부르면 404다")
		void myGuardianGetByNonPatient() throws Exception {
			link(patient1Token, guardian1Token);

			for (String token : List.of(guardian1Token, otherToken)) {
				getMyGuardian(token)
						.andExpect(status().isNotFound())
						.andExpect(jsonPath("$.success").value(false))
						.andExpect(jsonPath("$.message").value(MSG_LINK_NOT_FOUND));
			}
		}

		@Test
		@DisplayName("GUARDIAN·OTHER가 환자 쪽 해제를 부르면 404이고 연결은 그대로다")
		void myGuardianDeleteByNonPatient() throws Exception {
			link(patient1Token, guardian1Token);

			for (String token : List.of(guardian1Token, otherToken)) {
				disconnectMyGuardian(token)
						.andExpect(status().isNotFound())
						.andExpect(jsonPath("$.message").value(MSG_LINK_NOT_FOUND));
			}
			assertThat(linkRepository.findByPatientId(id("patient1"))).isPresent();
			assertThat(historyRepository.count()).isZero();
		}

		@Test
		@DisplayName("PATIENT·OTHER가 보호자 쪽 목록을 부르면 404다")
		void patientsGetByNonGuardian() throws Exception {
			for (String token : List.of(patient1Token, otherToken)) {
				getPatients(token)
						.andExpect(status().isNotFound())
						.andExpect(jsonPath("$.message").value(MSG_LINK_NOT_FOUND));
			}
		}

		@Test
		@DisplayName("PATIENT·OTHER가 보호자 쪽 해제를 부르면 404이고 연결은 그대로다 (연결된 환자 본인이 자기 id로 불러도)")
		void patientsDeleteByNonGuardian() throws Exception {
			link(patient1Token, guardian1Token);

			for (String token : List.of(patient1Token, otherToken)) {
				disconnectPatient(token, id("patient1"))
						.andExpect(status().isNotFound())
						.andExpect(jsonPath("$.message").value(MSG_LINK_NOT_FOUND));
			}
			assertThat(linkRepository.findByPatientId(id("patient1"))).isPresent();
			assertThat(historyRepository.count()).isZero();
		}

		@Test
		@DisplayName("토큰이 없으면 4개 API 모두 401이다")
		void noToken() throws Exception {
			mockMvc.perform(get(MY_GUARDIAN_URL)).andExpect(status().isUnauthorized());
			mockMvc.perform(delete(MY_GUARDIAN_URL)).andExpect(status().isUnauthorized());
			mockMvc.perform(get(PATIENTS_URL)).andExpect(status().isUnauthorized());
			mockMvc.perform(delete(PATIENTS_URL + "/1")).andExpect(status().isUnauthorized());
		}
	}

	@Nested
	@DisplayName("환자가 해제 - 연결이 지워지고 이력이 남는다 (2장, Q2, Q3)")
	class PatientDisconnect {

		@Test
		@DisplayName("data는 null이고, 연결이 지워지며 이력에 연결·해제 시각과 해제한 환자가 남는다")
		void disconnected() throws Exception {
			clock.setTime(NOW.plusMinutes(3));
			link(patient1Token, guardian1Token);
			clock.setTime(NOW.plusDays(2));

			disconnectMyGuardian(patient1Token)
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.success").value(true))
					.andExpect(jsonPath("$.data").value(nullValue()));

			assertThat(linkRepository.findByPatientId(id("patient1"))).isEmpty();
			List<GuardianLinkHistory> histories = histories();
			assertThat(histories).hasSize(1);
			assertHistory(histories.get(0), "patient1", "guardian1",
					NOW.plusMinutes(3), NOW.plusDays(2), "patient1");
		}

		@Test
		@DisplayName("해제 뒤에는 환자 조회가 null이고, 보호자 목록에서도 사라진다")
		void goneFromBothSides() throws Exception {
			link(patient1Token, guardian1Token);
			disconnectMyGuardian(patient1Token).andExpect(status().isOk());

			getMyGuardian(patient1Token).andExpect(jsonPath("$.data").value(nullValue()));
			getPatients(guardian1Token).andExpect(jsonPath("$.data", hasSize(0)));
		}

		@Test
		@DisplayName("보호자와 연결된 다른 환자는 그대로 남는다")
		void otherPatientKept() throws Exception {
			link(patient1Token, guardian1Token);
			link(patient2Token, guardian1Token);

			disconnectMyGuardian(patient1Token).andExpect(status().isOk());

			getPatients(guardian1Token)
					.andExpect(jsonPath("$.data", hasSize(1)))
					.andExpect(jsonPath("$.data[0].patientId").value(id("patient2")));
			assertThat(histories()).hasSize(1);
		}

		@Test
		@DisplayName("연결이 없으면 404이고 이력은 생기지 않는다")
		void noLink() throws Exception {
			disconnectMyGuardian(patient1Token)
					.andExpect(status().isNotFound())
					.andExpect(jsonPath("$.success").value(false))
					.andExpect(jsonPath("$.message").value(MSG_LINK_NOT_FOUND));

			assertThat(historyRepository.count()).isZero();
		}

		@Test
		@DisplayName("두 번 해제하면 두 번째는 404이고 이력은 1줄이다")
		void twice() throws Exception {
			link(patient1Token, guardian1Token);
			disconnectMyGuardian(patient1Token).andExpect(status().isOk());

			disconnectMyGuardian(patient1Token)
					.andExpect(status().isNotFound())
					.andExpect(jsonPath("$.message").value(MSG_LINK_NOT_FOUND));

			assertThat(histories()).hasSize(1);
		}
	}

	@Nested
	@DisplayName("보호자가 해제 - 자기 환자만 해제할 수 있다 (2장, 3.2, Q2, Q3, Q9)")
	class GuardianDisconnect {

		@Test
		@DisplayName("data는 null이고, 그 환자의 연결만 지워지며 이력에 해제한 보호자가 남는다")
		void disconnected() throws Exception {
			clock.setTime(NOW.plusMinutes(1));
			link(patient1Token, guardian1Token);
			clock.setTime(NOW.plusMinutes(2));
			link(patient2Token, guardian1Token);
			clock.setTime(NOW.plusDays(3));

			disconnectPatient(guardian1Token, id("patient1"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.success").value(true))
					.andExpect(jsonPath("$.data").value(nullValue()));

			assertThat(linkRepository.findByPatientId(id("patient1"))).isEmpty();
			assertThat(linkRepository.findByPatientId(id("patient2"))).isPresent();
			List<GuardianLinkHistory> histories = histories();
			assertThat(histories).hasSize(1);
			assertHistory(histories.get(0), "patient1", "guardian1",
					NOW.plusMinutes(1), NOW.plusDays(3), "guardian1");
		}

		@Test
		@DisplayName("해제 뒤에는 보호자 목록에서 사라지고, 환자 조회도 null이다")
		void goneFromBothSides() throws Exception {
			link(patient1Token, guardian1Token);
			disconnectPatient(guardian1Token, id("patient1")).andExpect(status().isOk());

			getPatients(guardian1Token).andExpect(jsonPath("$.data", hasSize(0)));
			getMyGuardian(patient1Token).andExpect(jsonPath("$.data").value(nullValue()));
		}

		@Test
		@DisplayName("제3자: 다른 보호자의 환자를 해제하면 404이고, 연결은 그대로며 이력도 없다")
		void otherGuardiansPatient() throws Exception {
			link(patient1Token, guardian1Token);

			disconnectPatient(guardian2Token, id("patient1"))
					.andExpect(status().isNotFound())
					.andExpect(jsonPath("$.message").value(MSG_LINK_NOT_FOUND));

			GuardianLink link = linkRepository.findByPatientId(id("patient1")).orElseThrow();
			assertThat(link.getGuardian().getId()).isEqualTo(id("guardian1"));
			assertThat(historyRepository.count()).isZero();
		}

		@Test
		@DisplayName("연결되지 않은 환자의 id면 404다")
		void notLinkedPatient() throws Exception {
			link(patient1Token, guardian1Token);

			disconnectPatient(guardian1Token, id("patient3"))
					.andExpect(status().isNotFound())
					.andExpect(jsonPath("$.message").value(MSG_LINK_NOT_FOUND));

			assertThat(historyRepository.count()).isZero();
		}

		@Test
		@DisplayName("없는 사용자 id나 환자가 아닌 사용자 id면 404다")
		void unknownOrNonPatientId() throws Exception {
			link(patient1Token, guardian1Token);
			Long unknownId = id("other1") + 1000;

			for (Long patientId : List.of(unknownId, id("guardian1"), id("other1"))) {
				disconnectPatient(guardian1Token, patientId)
						.andExpect(status().isNotFound())
						.andExpect(jsonPath("$.message").value(MSG_LINK_NOT_FOUND));
			}
			assertThat(linkRepository.findByPatientId(id("patient1"))).isPresent();
			assertThat(historyRepository.count()).isZero();
		}

		@Test
		@DisplayName("두 번 해제하면 두 번째는 404이고 이력은 1줄이다")
		void twice() throws Exception {
			link(patient1Token, guardian1Token);
			disconnectPatient(guardian1Token, id("patient1")).andExpect(status().isOk());

			disconnectPatient(guardian1Token, id("patient1"))
					.andExpect(status().isNotFound())
					.andExpect(jsonPath("$.message").value(MSG_LINK_NOT_FOUND));

			assertThat(histories()).hasSize(1);
		}
	}

	@Nested
	@DisplayName("해제 뒤 재연결 - 이력은 남고 다시 연결할 수 있다 (Q7, Q8)")
	class Reconnect {

		@Test
		@DisplayName("환자가 해제하면 코드를 다시 발급할 수 있고, 같은 보호자와 다시 연결되며 이전 이력은 남는다")
		void sameGuardianAfterPatientDisconnect() throws Exception {
			link(patient1Token, guardian1Token);
			mockMvc.perform(post(INVITE_URL).header("Authorization", patient1Token))
					.andExpect(status().isConflict())
					.andExpect(jsonPath("$.message").value(MSG_GUARDIAN_ALREADY_LINKED));

			clock.setTime(NOW.plusHours(1));
			disconnectMyGuardian(patient1Token).andExpect(status().isOk());
			clock.setTime(NOW.plusHours(2));
			String code = issuedCode(patient1Token);

			connect(guardian1Token, code)
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.data.connectedAt").value("2026-11-28T14:00:00"))
					.andExpect(jsonPath("$.data.access.meal").value("PRIVATE"));

			assertThat(linkRepository.findByPatientId(id("patient1")).orElseThrow().getConnectedAt())
					.isEqualTo(NOW.plusHours(2));
			List<GuardianLinkHistory> histories = histories();
			assertThat(histories).hasSize(1);
			assertHistory(histories.get(0), "patient1", "guardian1", NOW, NOW.plusHours(1), "patient1");
		}

		@Test
		@DisplayName("보호자가 해제하면 환자는 다른 보호자와 연결할 수 있다")
		void otherGuardianAfterGuardianDisconnect() throws Exception {
			link(patient1Token, guardian1Token);
			disconnectPatient(guardian1Token, id("patient1")).andExpect(status().isOk());

			link(patient1Token, guardian2Token);

			getMyGuardian(patient1Token)
					.andExpect(jsonPath("$.data.guardianNickname").value("보호자2"));
			getPatients(guardian1Token).andExpect(jsonPath("$.data", hasSize(0)));
			assertThat(histories()).hasSize(1);
		}

		@Test
		@DisplayName("같은 환자·보호자가 두 번 연결·해제하면 이력이 2줄 남는다")
		void twoHistories() throws Exception {
			link(patient1Token, guardian1Token);
			clock.setTime(NOW.plusHours(1));
			disconnectMyGuardian(patient1Token).andExpect(status().isOk());

			clock.setTime(NOW.plusHours(2));
			link(patient1Token, guardian1Token);
			clock.setTime(NOW.plusHours(3));
			disconnectPatient(guardian1Token, id("patient1")).andExpect(status().isOk());

			List<GuardianLinkHistory> histories = histories();
			assertThat(histories).hasSize(2);
			assertHistory(histories.get(0), "patient1", "guardian1", NOW, NOW.plusHours(1), "patient1");
			assertHistory(histories.get(1), "patient1", "guardian1",
					NOW.plusHours(2), NOW.plusHours(3), "guardian1");
		}

		@Test
		@DisplayName("환자 2명과 연결된 보호자가 1명을 해제하면 새 환자와 연결할 수 있다")
		void limitFreed() throws Exception {
			link(patient1Token, guardian1Token);
			link(patient2Token, guardian1Token);
			String code = issuedCode(patient3Token);
			connect(guardian1Token, code)
					.andExpect(status().isConflict())
					.andExpect(jsonPath("$.message").value(MSG_PATIENT_LIMIT));

			disconnectPatient(guardian1Token, id("patient1")).andExpect(status().isOk());

			connect(guardian1Token, code).andExpect(status().isOk());
			assertThat(linkRepository.countByGuardianId(id("guardian1"))).isEqualTo(2);
		}
	}

	@Nested
	@DisplayName("동시 해제 - 연결 행을 잠가 한 요청만 해제하고 나머지는 404다")
	class Concurrent {

		/** 요청을 한꺼번에 보내고 상태 코드를 모은다. */
		private List<Integer> runConcurrently(List<Callable<ResultActions>> requests) throws Exception {
			ExecutorService pool = Executors.newFixedThreadPool(requests.size());
			CountDownLatch ready = new CountDownLatch(requests.size());
			CountDownLatch start = new CountDownLatch(1);
			try {
				List<Future<ResultActions>> futures = new ArrayList<>();
				for (Callable<ResultActions> request : requests) {
					futures.add(pool.submit(() -> {
						ready.countDown();
						start.await();
						return request.call();
					}));
				}
				ready.await();
				start.countDown();
				List<Integer> statuses = new ArrayList<>();
				for (Future<ResultActions> future : futures) {
					statuses.add(future.get(30, TimeUnit.SECONDS).andReturn().getResponse().getStatus());
				}
				return statuses;
			} finally {
				pool.shutdownNow();
			}
		}

		@RepeatedTest(value = 5, name = "{displayName} {currentRepetition}/{totalRepetitions}")
		@DisplayName("환자와 보호자가 동시에 해제하면 하나는 200, 하나는 404이고 이력은 1줄이다")
		void patientAndGuardian() throws Exception {
			link(patient1Token, guardian1Token);
			Long patientId = id("patient1");

			List<Integer> statuses = runConcurrently(List.of(
					() -> disconnectMyGuardian(patient1Token),
					() -> disconnectPatient(guardian1Token, patientId)));

			assertThat(statuses).containsExactlyInAnyOrder(200, 404);
			assertThat(linkRepository.findByPatientId(patientId)).isEmpty();
			assertThat(historyRepository.count()).isEqualTo(1);
		}

		@RepeatedTest(value = 3, name = "{displayName} {currentRepetition}/{totalRepetitions}")
		@DisplayName("환자 2번·보호자 2번이 동시에 해제해도 200은 1개, 나머지는 404이고 이력은 1줄이다")
		void manyRequests() throws Exception {
			link(patient1Token, guardian1Token);
			Long patientId = id("patient1");

			List<Integer> statuses = runConcurrently(List.of(
					() -> disconnectMyGuardian(patient1Token),
					() -> disconnectMyGuardian(patient1Token),
					() -> disconnectPatient(guardian1Token, patientId),
					() -> disconnectPatient(guardian1Token, patientId)));

			assertThat(statuses).allMatch(s -> s == 200 || s == 404);
			assertThat(statuses).filteredOn(s -> s == 200).hasSize(1);
			assertThat(linkRepository.findByPatientId(patientId)).isEmpty();
			assertThat(historyRepository.count()).isEqualTo(1);
		}
	}
}
