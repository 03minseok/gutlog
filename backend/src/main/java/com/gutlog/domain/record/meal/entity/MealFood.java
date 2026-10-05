package com.gutlog.domain.record.meal.entity;

import com.gutlog.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "meal_food", uniqueConstraints = @UniqueConstraint(columnNames = {"food_id", "meal_record_id"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MealFood extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "food_id", nullable = false)
    private Food food;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meal_record_id", nullable = false)
    private MealRecord mealRecord;

    private MealFood(Food food, MealRecord mealRecord) {
        this.food = food;
        this.mealRecord = mealRecord;
    }

    public static MealFood of(Food food, MealRecord mealRecord) {
        return new MealFood(food, mealRecord);
    }
}
