package com.gutlog.domain.user.service;

import com.gutlog.domain.user.dto.request.LoginRequest;
import com.gutlog.domain.user.dto.request.SignupRequest;
import com.gutlog.domain.user.dto.response.LoginResponse;
import com.gutlog.domain.user.dto.response.UserResponse;
import com.gutlog.domain.user.entity.Authority;
import com.gutlog.domain.user.entity.IbdType;
import com.gutlog.domain.user.entity.Role;
import com.gutlog.domain.user.entity.User;
import com.gutlog.domain.user.repository.UserRepository;
import com.gutlog.global.config.JwtProvider;
import com.gutlog.global.exception.BusinessException;
import com.gutlog.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * UserService 단위 테스트.
 * 스프링 컨텍스트 없이 Mockito로 의존성(레포지토리, 인코더, JWT)을 가짜로 대체해서
 * 서비스의 비즈니스 로직만 빠르게 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtProvider jwtProvider;

    @InjectMocks UserService userService;

    private static User userWithId(Long id, String username, String encodedPassword) {
        User user = User.create(username, encodedPassword, "닉네임", Role.PATIENT, IbdType.ULCERATIVE_COLITIS);
        // id는 DB가 채워주는 값이라 단위 테스트에서는 리플렉션으로 넣어준다.
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private static ErrorCode errorCodeOf(Throwable e) {
        return ((BusinessException) e).getErrorCode();
    }

    @Nested
    @DisplayName("회원가입")
    class Signup {

        private final SignupRequest request = new SignupRequest(
                "gutlog", "password123", "장건강", Role.PATIENT, IbdType.ULCERATIVE_COLITIS);

        @Test
        @DisplayName("성공 - 비밀번호를 인코딩해서 저장하고 응답을 반환한다")
        void success() {
            given(userRepository.existsByUsername("gutlog")).willReturn(false);
            given(passwordEncoder.encode("password123")).willReturn("ENCODED");

            UserResponse response = userService.signup(request);

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            User saved = captor.getValue();

            assertThat(saved.getPassword()).isEqualTo("ENCODED"); // 평문 저장 금지
            assertThat(saved.getUsername()).isEqualTo("gutlog");
            assertThat(saved.getRole()).isEqualTo(Role.PATIENT);
            assertThat(saved.getIbdType()).isEqualTo(IbdType.ULCERATIVE_COLITIS);

            assertThat(response.username()).isEqualTo("gutlog");
            assertThat(response.nickname()).isEqualTo("장건강");
        }

        @Test
        @DisplayName("실패 - 이미 존재하는 아이디면 DUPLICATE_USERNAME 예외, 저장하지 않는다")
        void duplicateUsername() {
            given(userRepository.existsByUsername("gutlog")).willReturn(true);

            assertThatThrownBy(() -> userService.signup(request))
                    .isInstanceOf(BusinessException.class)
                    .extracting(UserServiceTest::errorCodeOf)
                    .isEqualTo(ErrorCode.DUPLICATE_USERNAME);

            verify(passwordEncoder, never()).encode(any());
            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("아이디 중복 체크")
    class CheckUsername {

        @Test
        @DisplayName("존재하지 않는 아이디면 사용 가능(true)")
        void available() {
            given(userRepository.existsByUsername("newbie")).willReturn(false);
            assertThat(userService.isUsernameAvailable("newbie")).isTrue();
        }

        @Test
        @DisplayName("이미 존재하는 아이디면 사용 불가(false)")
        void taken() {
            given(userRepository.existsByUsername("gutlog")).willReturn(true);
            assertThat(userService.isUsernameAvailable("gutlog")).isFalse();
        }
    }

    @Nested
    @DisplayName("로그인")
    class Login {

        private final LoginRequest request = new LoginRequest("gutlog", "password123");

        @Test
        @DisplayName("성공 - 토큰과 유저 정보를 반환한다")
        void success() {
            User user = userWithId(1L, "gutlog", "ENCODED");
            given(userRepository.findByUsername("gutlog")).willReturn(Optional.of(user));
            given(passwordEncoder.matches("password123", "ENCODED")).willReturn(true);
            given(jwtProvider.createToken(1L, Authority.USER)).willReturn("jwt-token");

            LoginResponse response = userService.login(request);

            assertThat(response.accessToken()).isEqualTo("jwt-token");
            assertThat(response.user().id()).isEqualTo(1L);
            assertThat(response.user().username()).isEqualTo("gutlog");
        }

        @Test
        @DisplayName("실패 - 없는 아이디면 LOGIN_FAILED")
        void userNotFound() {
            given(userRepository.findByUsername("gutlog")).willReturn(Optional.empty());

            assertThatThrownBy(() -> userService.login(request))
                    .isInstanceOf(BusinessException.class)
                    .extracting(UserServiceTest::errorCodeOf)
                    .isEqualTo(ErrorCode.LOGIN_FAILED);

            verify(jwtProvider, never()).createToken(anyLong(), any());
        }

        @Test
        @DisplayName("실패 - 비밀번호가 틀리면 LOGIN_FAILED (아이디 없음과 같은 에러로 계정 존재 여부를 숨긴다)")
        void wrongPassword() {
            User user = userWithId(1L, "gutlog", "ENCODED");
            given(userRepository.findByUsername("gutlog")).willReturn(Optional.of(user));
            given(passwordEncoder.matches("password123", "ENCODED")).willReturn(false);

            assertThatThrownBy(() -> userService.login(request))
                    .isInstanceOf(BusinessException.class)
                    .extracting(UserServiceTest::errorCodeOf)
                    .isEqualTo(ErrorCode.LOGIN_FAILED);

            verify(jwtProvider, never()).createToken(anyLong(), any());
        }
    }

    @Nested
    @DisplayName("내 정보 조회")
    class GetMyInfo {

        @Test
        @DisplayName("성공")
        void success() {
            given(userRepository.findById(1L)).willReturn(Optional.of(userWithId(1L, "gutlog", "ENCODED")));

            UserResponse response = userService.getMyInfo(1L);

            assertThat(response.id()).isEqualTo(1L);
            assertThat(response.username()).isEqualTo("gutlog");
        }

        @Test
        @DisplayName("실패 - 없는 유저면 USER_NOT_FOUND")
        void notFound() {
            given(userRepository.findById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getMyInfo(99L))
                    .isInstanceOf(BusinessException.class)
                    .extracting(UserServiceTest::errorCodeOf)
                    .isEqualTo(ErrorCode.USER_NOT_FOUND);
        }
    }
}
