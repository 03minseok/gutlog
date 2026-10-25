package com.gutlog.domain.record.meal.repository;

import com.gutlog.domain.record.meal.entity.Food;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FoodRepository extends JpaRepository<Food, Long> {
    Optional<Food> findByUserIdAndName(Long userId, String name);

    @Query("""
    select f.name from Food f
    left join MealFood mf on mf.food = f
    where f.user.id = :userId
    group by f.id, f.name
    order by count(mf) desc, f.name asc
    """)
    List<String> findNamesOrderByUsage(@Param("userId") Long userId);
}
