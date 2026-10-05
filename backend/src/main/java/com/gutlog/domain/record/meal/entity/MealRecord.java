package com.gutlog.domain.record.meal.entity;

import com.gutlog.domain.user.entity.User;
import com.gutlog.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "meal_record")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MealRecord extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String content;

    @Column(nullable = false)
    private LocalDateTime eatenAt;

    private MealRecord(User user, String content, LocalDateTime eatenAt) {
        this.user = user;
        this.content = content;
        this.eatenAt = eatenAt;
    }

    public static MealRecord create(User user, String content, LocalDateTime eatenAt) {
        return new MealRecord(user, content, eatenAt);
    }
}
