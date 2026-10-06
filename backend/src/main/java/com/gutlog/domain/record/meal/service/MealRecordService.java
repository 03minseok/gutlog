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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    public MealResponse uploadMealRecord(Long userId, MealCreateRequest request){
        User user = userRepository.findById(userId).orElseThrow(()->new BusinessException(ErrorCode.USER_NOT_FOUND));
        MealRecord mealRecord = request.toEntity(user);
        mealRecordRepository.save(mealRecord);
        linkFoods(mealRecord, request.foods());
        return MealResponse.of(mealRecord, request.foods());
    }

    //단일 조회
    public MealResponse getMealRecord(Long userId, Long mealRecordId){
        MealRecord mealRecord = findMyMeal(userId, mealRecordId);
			  List<String> foods = mealFoodRepository.findAllByMealRecordId(mealRecord.getId()).stream().map(f -> f.getFood().getName()).toList();
        return MealResponse.of(mealRecord, foods);
    }

    //전체 조회
    public List<MealResponse> getAllMealRecordByUser(Long userId, LocalDate from, LocalDate to){
        if(from.isAfter(to)){
            throw new BusinessException(ErrorCode.INVALID_DATE_RANGE);
        }
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.plusDays(1).atStartOfDay();

        // 기간 안의 내 식사 조회
        List<MealRecord> meals = mealRecordRepository
            .findAllByUserIdAndEatenAtGreaterThanEqualAndEatenAtLessThanOrderByEatenAtAsc(userId, start, end);

        if(meals.isEmpty()){
            return List.of();
        }

        // 식사 id만 뽑기
        List<Long> mealIds = meals.stream()
            .map(MealRecord::getId)
            .toList();

        // 그 식사들의 음식 연결을 한 번에 조회 (음식까지 같이)
        List<MealFood> mealFoods = mealFoodRepository.findAllByMealRecordIdIn(mealIds);

        // 식사 id별로 음식 이름 묶기
        Map<Long, List<String>> foodsByMeal = mealFoods.stream()
            .collect(Collectors.groupingBy(
                mf -> mf.getMealRecord().getId(),
                Collectors.mapping(mf -> mf.getFood().getName(), Collectors.toList())
            ));

        // 식사마다 응답 만들기 (DB 조회X)
        return meals.stream()
            .map(meal -> MealResponse.of(meal, foodsByMeal.getOrDefault(meal.getId(), List.of())))
            .toList();
    }

    //삭제
    @Transactional
    public void deleteMealRecord(Long userId, Long mealRecordId){
        MealRecord mealRecord = findMyMeal(userId, mealRecordId);
        mealFoodRepository.deleteAllByMealRecord(mealRecord);
        mealRecordRepository.delete(mealRecord);
    }

    //수정
    @Transactional
    public MealResponse editMealRecord(Long userId, Long mealRecordId, MealCreateRequest request){
        // mealRecord
        MealRecord mealRecord = findMyMeal(userId, mealRecordId);
        mealRecord.update(request.eatenAt());

        // mealFood
        mealFoodRepository.deleteAllByMealRecord(mealRecord);
        linkFoods(mealRecord, request.foods());
        return MealResponse.of(mealRecord, request.foods());
    }

    private MealRecord findMyMeal(Long userId, Long mealRecordId) {
        MealRecord mealRecord = mealRecordRepository.findById(mealRecordId)
            .orElseThrow(()->new BusinessException(ErrorCode.MEAL_RECORD_NOT_FOUND));
        if(!mealRecord.isOwnedBy(userId)){
            throw new BusinessException(ErrorCode.MEAL_RECORD_NOT_FOUND);
        }
        return mealRecord;
    }

    private void linkFoods(MealRecord mealRecord, List<String> foods) {
        for (String food : foods) {
            Food thisFood = foodRepository.findByUserIdAndName(mealRecord.getUser().getId(), food)
                .orElseGet(()->foodRepository.save(Food.create(mealRecord.getUser(), food)));
            mealFoodRepository.save(MealFood.of(thisFood, mealRecord));
        }
    }
}