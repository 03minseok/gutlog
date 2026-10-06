package com.gutlog.domain.record.meal.entity;

import com.gutlog.domain.user.entity.User;
import com.gutlog.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "food", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "name"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Food extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 50)
    private String name;

    private Food(User user, String name) {
        this.user = user;
        this.name = name;
    }

    public static String normalizeName(String name) {
        return name.strip();
    }

    public static Food create(User user, String name) {
        return new Food(user, normalizeName(name));
    }
}
