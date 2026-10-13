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

        String accessToken = jwtProvider.createToken(user.getId(), user.getAuthority());
        return LoginResponse.of(accessToken, UserResponse.fromEntity(user));
    }

    // me
    public UserResponse getMyInfo(Long userId){
        User user = userRepository.findById(userId)
                .orElseThrow(()->new BusinessException(ErrorCode.USER_NOT_FOUND));
        return UserResponse.fromEntity(user);
    }
}
