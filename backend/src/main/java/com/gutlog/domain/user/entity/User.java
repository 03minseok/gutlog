// [권한] 직접 작성 연습용. '// [권한]' 주석이 Claude가 작성했던 권한 코드이고, 지금 동작하는 코드는 권한 추가 전 상태다.
package com.gutlog.domain.user.entity;

import com.gutlog.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
// [권한] import org.hibernate.annotations.ColumnDefault;

@Entity
@Getter
@Table(name="users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(unique = true, nullable = false)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Enumerated(EnumType.STRING)
    private IbdType ibdType;

    // [권한] 기존 행이 있는 DB에 컬럼을 추가할 때도 NOT NULL을 만족하도록 DB 기본값을 둔다.
    // [권한] @Enumerated(EnumType.STRING)
    // [권한] @Column(nullable = false, length = 10)
    // [권한] @ColumnDefault("'USER'")
    // [권한] private Authority authority;

    private User(String username, String password, String nickname, Role role, IbdType ibdType){
        this.username = username;
        this.password = password;
        this.nickname = nickname;
        this.role = role;
        this.ibdType = ibdType;
        // [권한] this.authority = Authority.USER;   // 회원가입은 항상 USER. ADMIN은 DB에서 직접 지정
    }

    public static User create(String username, String password, String nickname, Role role, IbdType ibdType){
        return new User(username, password, nickname, role, ibdType);
    }
}
