package com.gutlog.domain.user.controller;

import com.gutlog.domain.user.dto.request.LoginRequest;
import com.gutlog.domain.user.dto.request.SignupRequest;
import com.gutlog.domain.user.dto.response.UserResponse;
import com.gutlog.domain.user.service.UserService;
import com.gutlog.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    //로그인
    @PostMapping("/login")
    public ApiResponse<UserResponse> login(@Valid @RequestBody LoginRequest request){
        return ApiResponse.success(userService.login(request));
    }

    //아이디 중복체크
    @GetMapping("/check-username")
    public ApiResponse<Boolean> isUsernameAvailable(@Valid @RequestParam String username){
        return ApiResponse.success(userService.isUsernameAvailable(username));
    }

    //회원가입
    @PostMapping("/signup")
    public ApiResponse<UserResponse> signup(@Valid @RequestBody SignupRequest request){
        return ApiResponse.success(userService.signup(request));
    }
}
