package com.gutlog.domain.user.entity;

import com.gutlog.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

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

    // 기존 행이 있는 DB에 컬럼을 추가할 때도 NOT NULL을 만족하도록 DB 기본값을 둔다.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @ColumnDefault("'USER'")
    private Authority authority;

    private User(String username, String password, String nickname, Role role, IbdType ibdType){
        this.username = username;
        this.password = password;
        this.nickname = nickname;
        this.role = role;
        this.ibdType = ibdType;
        this.authority = Authority.USER;
    }

    public static User create(String username, String password, String nickname, Role role, IbdType ibdType){
        return new User(username, password, nickname, role, ibdType);
    }
}
