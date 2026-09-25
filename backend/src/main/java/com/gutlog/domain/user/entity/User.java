package com.gutlog.domain.user.entity;

import com.gutlog.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

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

    private User(String username, String password, String nickname, Role role, IbdType ibdType){
        this.username = username;
        this.password = password;
        this.nickname = nickname;
        this.role = role;
        this.ibdType = ibdType;
    }

    public static User create(String username, String password, String nickname, Role role, IbdType ibdType){
        return new User(username, password, nickname, role, ibdType);
    }
}
