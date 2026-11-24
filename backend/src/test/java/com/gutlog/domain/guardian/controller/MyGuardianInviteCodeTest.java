package com.gutlog.domain.guardian.controller;

import com.gutlog.domain.guardian.entity.GuardianInviteCode;
import com.gutlog.domain.guardian.entity.GuardianLink;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 환자의 초대 코드 발급·재발급 통합 테스트 (G03, design.md 3.1·4.1, Q7).
 * 서버의 Clock을 바꿀 수 있는 시계로 대신해 만료 시각과 만료 경계를 확인한다.
 * 실제 요청 흐름과 같도록 @Transactional을 걸지 않고, 테스트가 끝날 때마다 데이터를 직접 지운다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(MyGuardianInviteCodeTest.MutableClockConfig.class)
class MyGuardianInviteCodeTest {

	private static final String URL = "/api/my-guardian/invite-code";
	private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 24, 12, 0);
	private static final String CODE_PATTERN = "^[A-Z0-9]{6}$";
	private static final String MSG_LINK_NOT_FOUND = "연결 정보를 찾을 수 없습니다.";
	private static final String MSG_ALREADY_LINKED = "연결된 보호자를 해제한 뒤 코드를 발급해 주세요.";

	@Autowired MockMvc mockMvc;
	@Autowired MutableClock clock;
	@Autowired UserRepository userRepository;
	@Autowired GuardianInviteCodeRepository inviteCodeRepository;
	@Autowired GuardianLinkRepository linkRepository;
	@Autowired GuardianLinkHistoryRepository historyRepository;
	@Autowired GuardianCodeAttemptRepository attemptRepository;

	private String patientToken;
	private String guardianToken;
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
		private Instant instant = Instant.now();

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
		patientToken = signupAndLogin("patient1", "환자", "PATIENT");
		guardianToken = signupAndLogin("guardian1", "보호자", "GUARDIAN");
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

	private ResultActions issue(String token) throws Exception {
		return mockMvc.perform(post(URL).header("Authorization", token));
	}

	private ResultActions current(String token) throws Exception {
		return mockMvc.perform(get(URL).header("Authorization", token));
	}

	private String issuedCode(String token) throws Exception {
		String body = issue(token).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		return JsonPath.read(body, "$.data.code");
	}

	private Long patientId() {
		return userRepository.findByUsername("patient1").orElseThrow().getId();
	}

	@Nested
	@DisplayName("POST /api/my-guardian/invite-code")
	class Issue {

		@Test
		@DisplayName("성공 - 6자리 영문 대문자+숫자 코드를 만들고, 만료 시각은 Clock 기준 10분 뒤다")
		void issueNew() throws Exception {
			issue(patientToken)
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.success").value(true))
					.andExpect(jsonPath("$.data.code").value(matchesPattern(CODE_PATTERN)))
					.andExpect(jsonPath("$.data.expiresAt").value("2026-11-24T12:10:00"));

			assertThat(inviteCodeRepository.count()).isEqualTo(1);
		}

		@Test
		@DisplayName("성공 - 재발급하면 새 코드와 새 만료 시각이 나오고, 이전 코드는 무효다")
		void reissue() throws Exception {
			String first = issuedCode(patientToken);
			clock.setTime(NOW.plusMinutes(3));

			String body = issue(patientToken)
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.data.code").value(matchesPattern(CODE_PATTERN)))
					.andExpect(jsonPath("$.data.expiresAt").value("2026-11-24T12:13:00"))
					.andReturn().getResponse().getContentAsString();
			String second = JsonPath.read(body, "$.data.code");

			assertThat(second).isNotEqualTo(first);
			assertThat(inviteCodeRepository.findByCode(first)).isEmpty();
			assertThat(inviteCodeRepository.count()).isEqualTo(1);
			GuardianInviteCode saved = inviteCodeRepository.findByPatientId(patientId()).orElseThrow();
			assertThat(saved.getCode()).isEqualTo(second);
		}

		@Test
		@DisplayName("성공 - 만료된 코드가 있어도 새로 발급하면 지금부터 10분 유효하다")
		void issueAfterExpired() throws Exception {
			issuedCode(patientToken);
			clock.setTime(NOW.plusMinutes(30));

			issue(patientToken)
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.data.expiresAt").value("2026-11-24T12:40:00"));

			assertThat(inviteCodeRepository.count()).isEqualTo(1);
		}

		@Test
		@DisplayName("실패 - GUARDIAN은 404다")
		void guardianNotFound() throws Exception {
			issue(guardianToken)
					.andExpect(status().isNotFound())
					.andExpect(jsonPath("$.message").value(MSG_LINK_NOT_FOUND));

			assertThat(inviteCodeRepository.count()).isZero();
		}

		@Test
		@DisplayName("실패 - OTHER는 404다")
		void otherNotFound() throws Exception {
			issue(otherToken)
					.andExpect(status().isNotFound())
					.andExpect(jsonPath("$.message").value(MSG_LINK_NOT_FOUND));

			assertThat(inviteCodeRepository.count()).isZero();
		}

		@Test
		@DisplayName("실패 - 토큰이 없으면 401이다")
		void unauthorized() throws Exception {
			mockMvc.perform(post(URL)).andExpect(status().isUnauthorized());
		}
	}

	@Nested
	@DisplayName("Q7 - 보호자가 연결된 환자는 해제한 뒤에만 발급할 수 있다")
	class AlreadyLinked {

		private GuardianLink link;

		@BeforeEach
		void link() {
			User patient = userRepository.findByUsername("patient1").orElseThrow();
			User guardian = userRepository.findByUsername("guardian1").orElseThrow();
			link = linkRepository.save(GuardianLink.create(patient, guardian, NOW.minusDays(1)));
		}

		@Test
		@DisplayName("실패 - 연결되어 있으면 409이고 코드가 만들어지지 않는다")
		void conflict() throws Exception {
			issue(patientToken)
					.andExpect(status().isConflict())
					.andExpect(jsonPath("$.message").value(MSG_ALREADY_LINKED));

			assertThat(inviteCodeRepository.count()).isZero();
		}

		@Test
		@DisplayName("성공 - 연결을 지운 뒤에는 발급된다")
		void issueAfterUnlink() throws Exception {
			linkRepository.delete(link);

			issue(patientToken)
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.data.code").value(matchesPattern(CODE_PATTERN)));
		}
	}

	@Nested
	@DisplayName("동시 요청 - 같은 환자가 동시에 발급해도 500 없이 코드는 1개다 (진 요청은 409여도 된다)")
	class Concurrent {

		private static final int REQUESTS = 5;

		/** 같은 환자 토큰으로 발급 요청을 한꺼번에 보낸다. */
		private List<MvcResult> issueConcurrently() throws Exception {
			ExecutorService pool = Executors.newFixedThreadPool(REQUESTS);
			CountDownLatch ready = new CountDownLatch(REQUESTS);
			CountDownLatch start = new CountDownLatch(1);
			try {
				List<Future<MvcResult>> futures = new ArrayList<>();
				for (int i = 0; i < REQUESTS; i++) {
					futures.add(pool.submit(() -> {
						ready.countDown();
						start.await();
						return issue(patientToken).andReturn();
					}));
				}
				ready.await();
				start.countDown();
				List<MvcResult> results = new ArrayList<>();
				for (Future<MvcResult> future : futures) {
					results.add(future.get(30, TimeUnit.SECONDS));
				}
				return results;
			} finally {
				pool.shutdownNow();
			}
		}

		/** 200 또는 409만 나오고, 행은 1개이며, 저장된 코드는 200을 받은 응답 중 하나의 코드다. */
		private void assertSingleCode(List<MvcResult> results) throws Exception {
			List<Integer> statuses = results.stream().map(r -> r.getResponse().getStatus()).toList();
			assertThat(statuses).allMatch(s -> s == 200 || s == 409).contains(200);

			List<String> okCodes = new ArrayList<>();
			for (MvcResult result : results) {
				if (result.getResponse().getStatus() == 200) {
					okCodes.add(JsonPath.read(result.getResponse().getContentAsString(), "$.data.code"));
				}
			}
			assertThat(inviteCodeRepository.count()).isEqualTo(1);
			String saved = inviteCodeRepository.findByPatientId(patientId()).orElseThrow().getCode();
			assertThat(okCodes).contains(saved);
		}

		@RepeatedTest(value = 5, name = "{displayName} {currentRepetition}/{totalRepetitions}")
		@DisplayName("첫 발급을 동시에 보내도 코드는 1개다")
		void firstIssue() throws Exception {
			assertSingleCode(issueConcurrently());
		}

		@RepeatedTest(value = 5, name = "{displayName} {currentRepetition}/{totalRepetitions}")
		@DisplayName("코드가 있을 때 재발급을 동시에 보내도 코드는 1개다")
		void reissue() throws Exception {
			String first = issuedCode(patientToken);

			assertSingleCode(issueConcurrently());
			assertThat(inviteCodeRepository.findByCode(first)).isEmpty();
		}
	}

	@Nested
	@DisplayName("GET /api/my-guardian/invite-code")
	class Current {

		@Test
		@DisplayName("성공 - 발급한 적이 없으면 null이다")
		void none() throws Exception {
			current(patientToken)
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.success").value(true))
					.andExpect(jsonPath("$.data").value(nullValue()));
		}

		@Test
		@DisplayName("성공 - 발급한 코드와 만료 시각을 돌려준다")
		void issued() throws Exception {
			String code = issuedCode(patientToken);

			current(patientToken)
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.data.code").value(code))
					.andExpect(jsonPath("$.data.expiresAt").value("2026-11-24T12:10:00"));
		}

		@Test
		@DisplayName("성공 - 재발급하면 새 코드만 보인다")
		void afterReissue() throws Exception {
			String first = issuedCode(patientToken);
			String second = issuedCode(patientToken);

			assertThat(second).isNotEqualTo(first);
			current(patientToken)
					.andExpect(jsonPath("$.data.code").value(second));
		}

		@Test
		@DisplayName("경계 - 만료 1초 전에는 보이고, 만료 시각과 같아지면 null이다")
		void expiryBoundary() throws Exception {
			String code = issuedCode(patientToken);

			clock.setTime(NOW.plusMinutes(10).minusSeconds(1));
			current(patientToken).andExpect(jsonPath("$.data.code").value(code));

			clock.setTime(NOW.plusMinutes(10));
			current(patientToken).andExpect(jsonPath("$.data").value(nullValue()));
		}

		@Test
		@DisplayName("실패 - GUARDIAN과 OTHER는 404다")
		void notPatient() throws Exception {
			current(guardianToken)
					.andExpect(status().isNotFound())
					.andExpect(jsonPath("$.message").value(MSG_LINK_NOT_FOUND));
			current(otherToken)
					.andExpect(status().isNotFound())
					.andExpect(jsonPath("$.message").value(MSG_LINK_NOT_FOUND));
		}
	}
}
