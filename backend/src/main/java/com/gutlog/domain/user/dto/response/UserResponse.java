package com.gutlog.domain.user.dto.response;

import com.gutlog.domain.user.entity.Authority;
import com.gutlog.domain.user.entity.IbdType;
import com.gutlog.domain.user.entity.Role;
import com.gutlog.domain.user.entity.User;

public record UserResponse(
        Long id,
        String username,
        String nickname,
        Role role,
        IbdType ibdType,
        Authority authority
) {
    public static UserResponse fromEntity(User user){
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getRole(),
                user.getIbdType(),
                user.getAuthority()
        );
    }
}
