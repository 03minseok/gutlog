package com.gutlog.domain.guardian.controller;

import com.gutlog.domain.guardian.entity.GuardianCodeAttempt;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 보호자의 초대 코드 입력으로 연결 통합 테스트 (G04, design.md 3.2·4.2·5.1·5.2, C1·C2, Q6·Q8).
 * 서버의 Clock을 바꿀 수 있는 시계로 대신해 만료와 차단 시간을 확인한다.
 * 실제 요청 흐름과 같도록 @Transactional을 걸지 않고, 테스트가 끝날 때마다 데이터를 직접 지운다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(GuardianConnectTest.MutableClockConfig.class)
class GuardianConnectTest {

	private static final String URL = "/api/guardian/patients";
	private static final String INVITE_URL = "/api/my-guardian/invite-code";
	private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 25, 12, 0);

	private static final String MSG_LINK_NOT_FOUND = "연결 정보를 찾을 수 없습니다.";
	private static final String MSG_CODE_NOT_FOUND = "초대 코드가 올바르지 않거나 만료되었습니다.";
	private static final String MSG_BLOCKED = "1분 뒤 다시 시도해 주세요.";
	private static final String MSG_PATIENT_LIMIT = "환자는 2명까지 연결할 수 있습니다.";
	private static final String MSG_PATIENT_ALREADY_LINKED = "이미 보호자가 연결된 환자입니다.";

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

	private ResultActions connect(String token, String code) throws Exception {
		return mockMvc.perform(post(URL)
				.header("Authorization", token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"code": "%s"}
						""".formatted(code)));
	}

	private ResultActions connectRaw(String token, String json) throws Exception {
		return mockMvc.perform(post(URL)
				.header("Authorization", token)
				.contentType(MediaType.APPLICATION_JSON)
				.content(json));
	}

	/** 형식은 맞지만 어떤 환자의 코드도 아닌 값 */
	private String wrongCode() {
		for (int i = 0; ; i++) {
			String candidate = "WRNG%02d".formatted(i);
			if (!inviteCodeRepository.existsByCode(candidate)) {
				return candidate;
			}
		}
	}

	private User user(String username) {
		return userRepository.findByUsername(username).orElseThrow();
	}

	/** DB에 남은 보호자의 실패 횟수. 행이 없으면 0으로 본다. */
	private int failCount(String guardianUsername) {
		return attemptRepository.findByGuardianId(user(guardianUsername).getId())
				.map(GuardianCodeAttempt::getFailCount)
				.orElse(0);
	}

	private void failTimes(String token, int times) throws Exception {
		for (int i = 0; i < times; i++) {
			connect(token, wrongCode());
		}
	}

	@Nested
	@DisplayName("성공 - 유효한 코드로 연결된다")
	class Success {

		@Test
		@DisplayName("환자 정보·연결 시각·공개 범위(전부 PRIVATE)를 돌려주고, 연결이 저장되며 코드는 지워진다")
		void connected() throws Exception {
			String code = issuedCode(patient1Token);
			clock.setTime(NOW.plusMinutes(3));

			connect(guardian1Token, code)
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.success").value(true))
					.andExpect(jsonPath("$.data.patientId").value(user("patient1").getId()))
					.andExpect(jsonPath("$.data.nickname").value("환자1"))
					.andExpect(jsonPath("$.data.connectedAt").value("2026-11-25T12:03:00"))
					.andExpect(jsonPath("$.data.access.meal").value("PRIVATE"))
					.andExpect(jsonPath("$.data.access.bowel").value("PRIVATE"))
					.andExpect(jsonPath("$.data.access.medication").value("PRIVATE"))
					.andExpect(jsonPath("$.data.access.weight").value("PRIVATE"))
					.andExpect(jsonPath("$.data.access.sleep").value("PRIVATE"));

			GuardianLink link = linkRepository.findByPatientIdAndGuardianId(
					user("patient1").getId(), user("guardian1").getId()).orElseThrow();
			assertThat(link.getConnectedAt()).isEqualTo(NOW.plusMinutes(3));
			assertThat(inviteCodeRepository.findByCode(code)).isEmpty();
			assertThat(inviteCodeRepository.findByPatientId(user("patient1").getId())).isEmpty();
		}

		@Test
		@DisplayName("같은 코드는 다시 쓸 수 없다 - 다른 보호자가 넣으면 404다")
		void codeIsOneTime() throws Exception {
			String code = issuedCode(patient1Token);
			connect(guardian1Token, code).andExpect(status().isOk());

			connect(guardian2Token, code)
					.andExpect(status().isNotFound())
					.andExpect(jsonPath("$.message").value(MSG_CODE_NOT_FOUND));

			assertThat(linkRepository.count()).isEqualTo(1);
			assertThat(linkRepository.findByPatientId(user("patient1").getId()).orElseThrow()
					.getGuardian().getId()).isEqualTo(user("guardian1").getId());
		}

		@Test
		@DisplayName("앞뒤 공백과 소문자가 섞여 있어도 연결된다 (C2)")
		void normalized() throws Exception {
			String code = issuedCode(patient1Token);

			connect(guardian1Token, "  " + code.toLowerCase(Locale.ROOT) + " ")
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.data.nickname").value("환자1"));
		}

		@Test
		@DisplayName("재발급하면 이전 코드로는 연결되지 않는다")
		void reissuedOldCode() throws Exception {
			String first = issuedCode(patient1Token);
			String second = issuedCode(patient1Token);

			connect(guardian1Token, first).andExpect(status().isNotFound());
			connect(guardian1Token, second).andExpect(status().isOk());
		}

		@Test
		@DisplayName("보호자 1명은 환자 2명까지 연결할 수 있다")
		void twoPatients() throws Exception {
			connect(guardian1Token, issuedCode(patient1Token)).andExpect(status().isOk());
			connect(guardian1Token, issuedCode(patient2Token)).andExpect(status().isOk());

			assertThat(linkRepository.countByGuardianId(user("guardian1").getId())).isEqualTo(2);
		}
	}

	@Nested
	@DisplayName("형식 오류 - 400이고 실패 횟수에 넣지 않는다 (C2)")
	class Format {

		@ParameterizedTest(name = "\"{0}\"")
		@ValueSource(strings = {"ABC12", "ABC1234", "ABC-12", "ABC 12", "가나다라마바", "", "   "})
		@DisplayName("6자리 영문 대문자+숫자가 아니면 400이다")
		void invalid(String code) throws Exception {
			connect(guardian1Token, code).andExpect(status().isBadRequest());

			assertThat(failCount("guardian1")).isZero();
		}

		@Test
		@DisplayName("code가 null이거나 없으면 400이다")
		void missing() throws Exception {
			connectRaw(guardian1Token, "{\"code\": null}").andExpect(status().isBadRequest());
			connectRaw(guardian1Token, "{}").andExpect(status().isBadRequest());

			assertThat(failCount("guardian1")).isZero();
		}

		@Test
		@DisplayName("형식 오류를 3번 내도 막히지 않고, 맞는 코드로 연결된다")
		void notCounted() throws Exception {
			String code = issuedCode(patient1Token);
			for (int i = 0; i < 3; i++) {
				connect(guardian1Token, "ABC").andExpect(status().isBadRequest());
			}

			connect(guardian1Token, code).andExpect(status().isOk());
		}
	}

	@Nested
	@DisplayName("만료 - 발급 후 10분이 되면 404다")
	class Expiry {

		@Test
		@DisplayName("경계 - 만료 1초 전에는 연결된다")
		void beforeExpiry() throws Exception {
			String code = issuedCode(patient1Token);
			clock.setTime(NOW.plusMinutes(10).minusSeconds(1));

			connect(guardian1Token, code).andExpect(status().isOk());
		}

		@Test
		@DisplayName("경계 - 만료 시각과 같아지면 404이고 실패 횟수가 오른다")
		void atExpiry() throws Exception {
			String code = issuedCode(patient1Token);
			clock.setTime(NOW.plusMinutes(10));

			connect(guardian1Token, code)
					.andExpect(status().isNotFound())
					.andExpect(jsonPath("$.message").value(MSG_CODE_NOT_FOUND));

			assertThat(linkRepository.count()).isZero();
			assertThat(failCount("guardian1")).isEqualTo(1);
		}
	}

	@Nested
	@DisplayName("역할 - GUARDIAN이 아니면 404다")
	class RoleCheck {

		@Test
		@DisplayName("PATIENT는 404이고, 코드는 그대로 남으며 실패 횟수도 생기지 않는다")
		void patient() throws Exception {
			String code = issuedCode(patient1Token);

			connect(patient2Token, code)
					.andExpect(status().isNotFound())
					.andExpect(jsonPath("$.message").value(MSG_LINK_NOT_FOUND));

			assertThat(linkRepository.count()).isZero();
			assertThat(inviteCodeRepository.findByCode(code)).isPresent();
			assertThat(attemptRepository.count()).isZero();
		}

		@Test
		@DisplayName("OTHER는 404이고, 코드는 그대로 남으며 실패 횟수도 생기지 않는다")
		void other() throws Exception {
			String code = issuedCode(patient1Token);

			connect(otherToken, code)
					.andExpect(status().isNotFound())
					.andExpect(jsonPath("$.message").value(MSG_LINK_NOT_FOUND));

			assertThat(linkRepository.count()).isZero();
			assertThat(inviteCodeRepository.findByCode(code)).isPresent();
			assertThat(attemptRepository.count()).isZero();
		}

		@Test
		@DisplayName("토큰이 없으면 401이다")
		void unauthorized() throws Exception {
			mockMvc.perform(post(URL)
							.contentType(MediaType.APPLICATION_JSON)
							.content("{\"code\": \"ABC123\"}"))
					.andExpect(status().isUnauthorized());
		}
	}

	@Nested
	@DisplayName("개수 - 환자당 보호자 1명, 보호자당 환자 2명 (409)")
	class Limit {

		@Test
		@DisplayName("이미 보호자가 있는 환자의 유효한 코드는 409이고, 기존 연결은 그대로이며 실패 횟수에 넣지 않는다 (4.2의 6단계)")
		void patientAlreadyLinked() throws Exception {
			// 재발급과 연결이 동시에 일어나 연결된 환자에게 유효한 코드가 남은 상태를 직접 만든다
			User patient = user("patient1");
			linkRepository.save(GuardianLink.create(patient, user("guardian2"), NOW.minusDays(1)));
			inviteCodeRepository.save(GuardianInviteCode.create(patient, "LINKED", NOW.plusMinutes(10)));

			connect(guardian1Token, "LINKED")
					.andExpect(status().isConflict())
					.andExpect(jsonPath("$.message").value(MSG_PATIENT_ALREADY_LINKED));

			assertThat(linkRepository.count()).isEqualTo(1);
			assertThat(linkRepository.findByPatientId(patient.getId()).orElseThrow()
					.getGuardian().getId()).isEqualTo(user("guardian2").getId());
			assertThat(failCount("guardian1")).isZero();
		}

		@Test
		@DisplayName("보호자가 이미 2명과 연결되어 있으면 3번째 코드는 409이고, 그 코드는 그대로 남으며 실패 횟수에 넣지 않는다")
		void guardianLimit() throws Exception {
			connect(guardian1Token, issuedCode(patient1Token)).andExpect(status().isOk());
			connect(guardian1Token, issuedCode(patient2Token)).andExpect(status().isOk());
			String third = issuedCode(patient3Token);

			connect(guardian1Token, third)
					.andExpect(status().isConflict())
					.andExpect(jsonPath("$.message").value(MSG_PATIENT_LIMIT));

			assertThat(linkRepository.countByGuardianId(user("guardian1").getId())).isEqualTo(2);
			assertThat(linkRepository.findByPatientId(user("patient3").getId())).isEmpty();
			assertThat(inviteCodeRepository.findByCode(third)).isPresent();
			assertThat(failCount("guardian1")).isZero();
		}

		@Test
		@DisplayName("보호자가 이미 2명과 연결되어 있으면 코드를 확인하지 않는다 - 틀린 코드도 404가 아니라 409다")
		void limitCheckedBeforeCode() throws Exception {
			connect(guardian1Token, issuedCode(patient1Token)).andExpect(status().isOk());
			connect(guardian1Token, issuedCode(patient2Token)).andExpect(status().isOk());

			for (int i = 0; i < 3; i++) {
				connect(guardian1Token, wrongCode())
						.andExpect(status().isConflict())
						.andExpect(jsonPath("$.message").value(MSG_PATIENT_LIMIT));
			}
			assertThat(failCount("guardian1")).isZero();
		}
	}

	@Nested
	@DisplayName("입력 차단 - 3번 틀리면 429로 1분 막는다 (C1)")
	class Block {

		@Test
		@DisplayName("1·2번째 오답은 404, 3번째 오답은 429다")
		void thirdFailureBlocked() throws Exception {
			connect(guardian1Token, wrongCode())
					.andExpect(status().isNotFound())
					.andExpect(jsonPath("$.message").value(MSG_CODE_NOT_FOUND));
			connect(guardian1Token, wrongCode())
					.andExpect(status().isNotFound());
			connect(guardian1Token, wrongCode())
					.andExpect(status().isTooManyRequests())
					.andExpect(jsonPath("$.success").value(false))
					.andExpect(jsonPath("$.message").value(MSG_BLOCKED));
		}

		@Test
		@DisplayName("실패 횟수와 차단 시각이 DB에 남는다 - 응답이 실패여도 롤백되지 않는다 (5.1)")
		void persistedDespiteFailure() throws Exception {
			connect(guardian1Token, wrongCode()).andExpect(status().isNotFound());
			assertThat(failCount("guardian1")).isEqualTo(1);

			connect(guardian1Token, wrongCode()).andExpect(status().isNotFound());
			assertThat(failCount("guardian1")).isEqualTo(2);

			clock.setTime(NOW.plusSeconds(30));
			connect(guardian1Token, wrongCode()).andExpect(status().isTooManyRequests());
			GuardianCodeAttempt attempt = attemptRepository.findByGuardianId(user("guardian1").getId()).orElseThrow();
			assertThat(attempt.getBlockedUntil()).isEqualTo(NOW.plusSeconds(30).plusMinutes(1));
		}

		@Test
		@DisplayName("막힌 동안에는 맞는 코드도 429이고, 코드는 쓰이지 않는다")
		void validCodeWhileBlocked() throws Exception {
			String code = issuedCode(patient1Token);
			failTimes(guardian1Token, 3);

			connect(guardian1Token, code)
					.andExpect(status().isTooManyRequests())
					.andExpect(jsonPath("$.message").value(MSG_BLOCKED));

			assertThat(linkRepository.count()).isZero();
			assertThat(inviteCodeRepository.findByCode(code)).isPresent();
		}

		@Test
		@DisplayName("경계 - 1분이 되기 1초 전까지는 429, 1분이 지나면 맞는 코드로 연결된다")
		void unblockedAfterOneMinute() throws Exception {
			failTimes(guardian1Token, 3);
			clock.setTime(NOW.plusMinutes(1).minusSeconds(1));
			String code = issuedCode(patient1Token);

			connect(guardian1Token, code).andExpect(status().isTooManyRequests());

			clock.setTime(NOW.plusMinutes(1));
			connect(guardian1Token, code).andExpect(status().isOk());
		}

		@Test
		@DisplayName("1분이 지나면 횟수는 0부터 다시 센다 - 다시 3번째 오답에서 429다")
		void countRestartsAfterBlock() throws Exception {
			failTimes(guardian1Token, 3);
			clock.setTime(NOW.plusMinutes(1));

			connect(guardian1Token, wrongCode()).andExpect(status().isNotFound());
			connect(guardian1Token, wrongCode()).andExpect(status().isNotFound());
			connect(guardian1Token, wrongCode()).andExpect(status().isTooManyRequests());
		}

		@Test
		@DisplayName("연결에 성공하면 횟수가 0이 된다 - 그 뒤 2번 틀려도 막히지 않는다")
		void resetOnSuccess() throws Exception {
			failTimes(guardian1Token, 2);
			connect(guardian1Token, issuedCode(patient1Token)).andExpect(status().isOk());
			assertThat(failCount("guardian1")).isZero();

			connect(guardian1Token, wrongCode()).andExpect(status().isNotFound());
			connect(guardian1Token, wrongCode()).andExpect(status().isNotFound());
		}

		@Test
		@DisplayName("실패 횟수는 보호자별이다 - 한 보호자가 막혀도 다른 보호자는 연결된다")
		void perGuardian() throws Exception {
			failTimes(guardian1Token, 3);

			connect(guardian2Token, issuedCode(patient1Token)).andExpect(status().isOk());
			assertThat(failCount("guardian2")).isZero();
		}
	}

	@Nested
	@DisplayName("동시 요청 - 500 없이 연결 수 제한을 지킨다 (5.2)")
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

		@RepeatedTest(value = 3, name = "{displayName} {currentRepetition}/{totalRepetitions}")
		@DisplayName("보호자 5명이 같은 코드를 동시에 넣어도 연결은 1개이고, 나머지는 404다")
		void sameCodeManyGuardians() throws Exception {
			String code = issuedCode(patient1Token);
			List<String> tokens = new ArrayList<>(List.of(guardian1Token, guardian2Token));
			for (int i = 3; i <= 5; i++) {
				tokens.add(signupAndLogin("guardian" + i, "보호자" + i, "GUARDIAN"));
			}

			List<Callable<ResultActions>> requests = new ArrayList<>();
			for (String token : tokens) {
				requests.add(() -> connect(token, code));
			}
			List<Integer> statuses = runConcurrently(requests);

			assertThat(statuses).allMatch(s -> s == 200 || s == 404);
			assertThat(statuses).filteredOn(s -> s == 200).hasSize(1);
			assertThat(linkRepository.count()).isEqualTo(1);
			assertThat(inviteCodeRepository.findByCode(code)).isEmpty();
		}

		@RepeatedTest(value = 3, name = "{displayName} {currentRepetition}/{totalRepetitions}")
		@DisplayName("한 보호자가 다른 환자의 코드 3개를 동시에 넣어도 연결은 2개이고, 나머지 1개는 409다")
		void threeCodesOneGuardian() throws Exception {
			List<String> codes = List.of(
					issuedCode(patient1Token), issuedCode(patient2Token), issuedCode(patient3Token));

			List<Callable<ResultActions>> requests = new ArrayList<>();
			for (String code : codes) {
				requests.add(() -> connect(guardian1Token, code));
			}
			List<Integer> statuses = runConcurrently(requests);

			assertThat(statuses).allMatch(s -> s == 200 || s == 409);
			assertThat(statuses).filteredOn(s -> s == 200).hasSize(2);
			assertThat(linkRepository.countByGuardianId(user("guardian1").getId())).isEqualTo(2);
		}
	}
}
