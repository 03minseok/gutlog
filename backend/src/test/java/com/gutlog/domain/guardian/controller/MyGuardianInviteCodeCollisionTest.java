package com.gutlog.domain.guardian.controller;

import com.gutlog.domain.guardian.repository.GuardianInviteCodeRepository;
import com.gutlog.domain.guardian.repository.GuardianLinkRepository;
import com.gutlog.domain.guardian.service.InviteCodeGenerator;
import com.gutlog.domain.user.repository.UserRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 초대 코드 겹침 처리 (design.md 4.1 "다른 행의 코드와 겹치면 다시 만든다").
 * 무작위로는 겹침을 재현할 수 없어서, 생성기를 정해진 코드를 차례로 내는 가짜로 바꾼다.
 * 서비스는 InviteCodeGenerator.generate()로만 코드를 만든다는 전제다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(MyGuardianInviteCodeTest.MutableClockConfig.class)
class MyGuardianInviteCodeCollisionTest {

	private static final String URL = "/api/my-guardian/invite-code";
	private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 24, 12, 0);

	@Autowired MockMvc mockMvc;
	@Autowired MyGuardianInviteCodeTest.MutableClock clock;
	@Autowired UserRepository userRepository;
	@Autowired GuardianInviteCodeRepository inviteCodeRepository;
	@Autowired GuardianLinkRepository linkRepository;

	@MockitoBean InviteCodeGenerator generator;

	private String patientAToken;
	private String patientBToken;

	@BeforeEach
	void setUp() throws Exception {
		clock.setTime(NOW);
		patientAToken = signupAndLogin("patienta", "환자A");
		patientBToken = signupAndLogin("patientb", "환자B");
	}

	@AfterEach
	void cleanUp() {
		inviteCodeRepository.deleteAllInBatch();
		linkRepository.deleteAllInBatch();
		userRepository.deleteAllInBatch();
	}

	private String signupAndLogin(String username, String nickname) throws Exception {
		mockMvc.perform(post("/api/users/signup")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"username": "%s", "password": "password123", "nickname": "%s", "role": "PATIENT", "ibdType": "CROHNS_DISEASE"}
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

	private ResultActions issue(String token) throws Exception {
		return mockMvc.perform(post(URL).header("Authorization", token));
	}

	private String savedCodeOf(String username) {
		Long id = userRepository.findByUsername(username).orElseThrow().getId();
		return inviteCodeRepository.findByPatientId(id).orElseThrow().getCode();
	}

	@Test
	@DisplayName("다른 환자의 코드와 겹치면 다시 만들고, 그 환자의 코드는 그대로다")
	void collideWithOtherPatient() throws Exception {
		given(generator.generate()).willReturn("AAAAAA", "AAAAAA", "BBBBBB");

		issue(patientAToken).andExpect(status().isOk()).andExpect(jsonPath("$.data.code").value("AAAAAA"));
		issue(patientBToken).andExpect(status().isOk()).andExpect(jsonPath("$.data.code").value("BBBBBB"));

		assertThat(savedCodeOf("patienta")).isEqualTo("AAAAAA");
		assertThat(savedCodeOf("patientb")).isEqualTo("BBBBBB");
	}

	@Test
	@DisplayName("다른 환자의 만료된 코드와 겹쳐도 다시 만든다 (만료된 행도 남아 있다)")
	void collideWithExpiredCode() throws Exception {
		given(generator.generate()).willReturn("AAAAAA", "AAAAAA", "BBBBBB");

		issue(patientAToken).andExpect(status().isOk());
		clock.setTime(NOW.plusMinutes(30));
		issue(patientBToken).andExpect(status().isOk()).andExpect(jsonPath("$.data.code").value("BBBBBB"));

		assertThat(savedCodeOf("patienta")).isEqualTo("AAAAAA");
	}

	@Test
	@DisplayName("재발급 때 자기 이전 코드와 같은 값이 나오면 다시 만든다 (이전 코드는 무효여야 한다)")
	void collideWithOwnPreviousCode() throws Exception {
		given(generator.generate()).willReturn("AAAAAA", "AAAAAA", "CCCCCC");

		issue(patientAToken).andExpect(status().isOk()).andExpect(jsonPath("$.data.code").value("AAAAAA"));
		issue(patientAToken).andExpect(status().isOk()).andExpect(jsonPath("$.data.code").value("CCCCCC"));

		assertThat(inviteCodeRepository.findByCode("AAAAAA")).isEmpty();
		assertThat(inviteCodeRepository.count()).isEqualTo(1);
	}
}
