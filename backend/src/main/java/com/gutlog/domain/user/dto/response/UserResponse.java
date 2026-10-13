// [권한] 직접 작성 연습용. '// [권한]' 주석이 Claude가 작성했던 권한 코드이고, 지금 동작하는 코드는 권한 추가 전 상태다.
package com.gutlog.domain.user.dto.response;

// [권한] import com.gutlog.domain.user.entity.Authority;
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
    Authority authority   // 프론트가 관리자 메뉴를 보여줄지 판단할 때 쓴다
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
