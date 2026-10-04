package com.gutlog.domain.user.dto;

import com.gutlog.domain.user.dto.request.SignupRequest;
import com.gutlog.domain.user.entity.IbdType;
import com.gutlog.domain.user.entity.Role;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SignupRequest의 Bean Validation 규칙 테스트.
 * 스프링 없이 Validator를 직접 만들어서 어노테이션 규칙만 검증한다.
 */
class SignupRequestValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    private Set<String> messages(SignupRequest request) {
        return validator.validate(request).stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toSet());
    }

    @Test
    @DisplayName("정상 요청은 위반 사항이 없다")
    void valid() {
        SignupRequest request = new SignupRequest("gutlog", "password123", "장건강", Role.PATIENT, IbdType.ULCERATIVE_COLITIS);
        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    @DisplayName("환자(PATIENT)가 질환 종류를 선택하지 않으면 실패")
    void patientWithoutIbdType() {
        SignupRequest request = new SignupRequest("gutlog", "password123", "장건강", Role.PATIENT, null);
        assertThat(messages(request)).containsExactly("환자는 질환 종류를 선택해야 합니다.");
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = Role.class, names = {"GUARDIAN", "OTHER"})
    @DisplayName("환자가 아니면 질환 종류 없이 가입 가능")
    void nonPatientWithoutIbdType(Role role) {
        SignupRequest request = new SignupRequest("gutlog", "password123", "장건강", role, null);
        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    @DisplayName("role이 없으면 실패")
    void roleNull() {
        SignupRequest request = new SignupRequest("gutlog", "password123", "장건강", null, null);
        assertThat(messages(request)).contains("유저 타입 선택은 필수입니다.");
    }

    @ParameterizedTest
    @ValueSource(strings = {"ab", "abcdefghijklmnopqrstu"}) // 2자, 21자
    @DisplayName("아이디 길이가 3~20자를 벗어나면 실패")
    void usernameLength(String username) {
        SignupRequest request = new SignupRequest(username, "password123", "장건강", Role.GUARDIAN, null);
        assertThat(messages(request)).contains("아이디는 3자 이상 20자 이하로 입력해주세요.");
    }

    @ParameterizedTest
    @ValueSource(strings = {"1234567", "123456789012345678901"}) // 7자, 21자
    @DisplayName("비밀번호 길이가 8~20자를 벗어나면 실패")
    void passwordLength(String password) {
        SignupRequest request = new SignupRequest("gutlog", password, "장건강", Role.GUARDIAN, null);
        assertThat(messages(request)).contains("비밀번호는 8자 이상 20자 이하로 입력해주세요.");
    }

    @Test
    @DisplayName("닉네임이 8자를 넘으면 실패")
    void nicknameTooLong() {
        SignupRequest request = new SignupRequest("gutlog", "password123", "가나다라마바사아자", Role.GUARDIAN, null);
        assertThat(messages(request)).contains("닉네임은 8자 이하로 입력해주세요.");
    }

    @Test
    @DisplayName("공백만 있는 문자열은 NotBlank에 걸린다")
    void blank() {
        SignupRequest request = new SignupRequest("   ", "password123", "  ", Role.GUARDIAN, null);
        assertThat(messages(request)).contains("아이디는 필수입니다.", "닉네임은 필수입니다.");
    }
}
