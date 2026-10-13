// [권한] 직접 작성 연습용. '// [권한]' 주석이 Claude가 작성했던 권한 코드이고, 지금 동작하는 코드는 권한 추가 전 상태다.
package com.gutlog.domain.user.service;

import com.gutlog.domain.user.dto.request.LoginRequest;
import com.gutlog.domain.user.dto.request.SignupRequest;
import com.gutlog.domain.user.dto.response.LoginResponse;
import com.gutlog.domain.user.dto.response.UserResponse;
import com.gutlog.domain.user.entity.User;
import com.gutlog.domain.user.repository.UserRepository;
import com.gutlog.global.config.JwtProvider;
import com.gutlog.global.exception.BusinessException;
import com.gutlog.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    //회원가입
    @Transactional
    public UserResponse signup(SignupRequest request){
        if(userRepository.existsByUsername(request.username())){
            throw new BusinessException(ErrorCode.DUPLICATE_USERNAME);
        }
        String encodedPassword = passwordEncoder.encode(request.password());
        User user = request.toEntity(encodedPassword);
        userRepository.save(user);
        return UserResponse.fromEntity(user);
    }

    public boolean isUsernameAvailable(String username){
        return !userRepository.existsByUsername(username);
    }

    //로그인
    public LoginResponse login(LoginRequest request){
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(()-> new BusinessException(ErrorCode.LOGIN_FAILED));

        if(!passwordEncoder.matches(request.password(), user.getPassword())){
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }

        // [권한] String accessToken = jwtProvider.createToken(user.getId(), user.getAuthority());   // 토큰에 권한도 담는다
        String accessToken = jwtProvider.createToken(user.getId());
        return LoginResponse.of(accessToken, UserResponse.fromEntity(user));
    }

    // me
    public UserResponse getMyInfo(Long userId){
        User user = userRepository.findById(userId)
                .orElseThrow(()->new BusinessException(ErrorCode.USER_NOT_FOUND));
        return UserResponse.fromEntity(user);
    }
}
