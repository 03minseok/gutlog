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
@Table(name = "meal_record",
    indexes = @Index(name="idx_meal_record_user_eaten", columnList = "user_id, eaten_at"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MealRecord extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDateTime eatenAt;

    private MealRecord(User user, LocalDateTime eatenAt) {
        this.user = user;
        this.eatenAt = eatenAt;
    }

    public static MealRecord create(User user, LocalDateTime eatenAt) {
        return new MealRecord(user, eatenAt);
    }

    public void update(LocalDateTime eatenAt){
        this.eatenAt = eatenAt;
    }

    public boolean isOwnedBy(Long userId) {
        return this.user.getId().equals(userId);
    }
}
