package com.gutlog.domain.record.meal.service;

import com.gutlog.domain.record.meal.dto.request.MealCreateRequest;
import com.gutlog.domain.record.meal.dto.response.MealResponse;
import com.gutlog.domain.record.meal.entity.Food;
import com.gutlog.domain.record.meal.entity.MealFood;
import com.gutlog.domain.record.meal.entity.MealRecord;
import com.gutlog.domain.record.meal.repository.FoodRepository;
import com.gutlog.domain.record.meal.repository.MealFoodRepository;
import com.gutlog.domain.record.meal.repository.MealRecordRepository;
import com.gutlog.domain.user.entity.User;
import com.gutlog.domain.user.repository.UserRepository;
import com.gutlog.global.exception.BusinessException;
import com.gutlog.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MealRecordService {

    private final UserRepository userRepository;
    private final MealRecordRepository mealRecordRepository;
    private final FoodRepository foodRepository;
    private final MealFoodRepository mealFoodRepository;

    //등록
    @Transactional
    public MealResponse uploadMealRecord(MealCreateRequest request, Long userId){
        User user = userRepository.findById(userId).orElseThrow(()->new BusinessException(ErrorCode.USER_NOT_FOUND));
        MealRecord mealRecord = request.toEntity(user);
        mealRecordRepository.save(mealRecord);
        for (String food : request.foods()) {
            Food thisFood = foodRepository.findByUserIdAndName(userId, food)
                    .orElseGet(()->foodRepository.save(Food.create(user, food)));
            mealFoodRepository.save(MealFood.of(thisFood, mealRecord));
        }
        return MealResponse.of(mealRecord, request.foods());
    }

    //단일 조회
    public MealResponse getMealRecord(Long mealRecordId, Long userId){
        MealRecord mealRecord = mealRecordRepository.findById(mealRecordId)
            .orElseThrow(()->new BusinessException(ErrorCode.MEAL_RECORD_NOT_FOUND));
        if (!mealRecord.isOwnedBy(userId)) {
            throw new BusinessException(ErrorCode.MEAL_RECORD_NOT_FOUND);
        }
			  List<String> foods = mealFoodRepository.findAllByMealRecordId(mealRecord.getId()).stream().map(f -> f.getFood().getName()).toList();
        return MealResponse.of(mealRecord, foods);
    }

    //전체 조회


    //삭제

    //수정

}
