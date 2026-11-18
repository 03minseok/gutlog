package com.gutlog.domain.record.weight.service;

import com.gutlog.domain.record.weight.dto.request.WeightRequest;
import com.gutlog.domain.record.weight.dto.response.WeightResponse;
import com.gutlog.domain.record.weight.entity.WeightRecord;
import com.gutlog.domain.record.weight.repository.WeightRecordRepository;
import com.gutlog.domain.user.entity.User;
import com.gutlog.domain.user.repository.UserRepository;
import com.gutlog.global.exception.BusinessException;
import com.gutlog.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WeightService {
	private final WeightRecordRepository weightRecordRepository;
	private final UserRepository userRepository;
	private final Clock clock;

	//기간 조회 (from, to 모두 포함, 날짜순)
	public List<WeightResponse> getWeightRecordsByPeriod(Long userId, LocalDate from, LocalDate to) {
		if (from.isAfter(to)) {
			throw new BusinessException(ErrorCode.INVALID_DATE_RANGE);
		}
		return weightRecordRepository.findAllByUserIdAndRecordDateBetweenOrderByRecordDateAsc(userId, from, to)
				.stream().map(WeightResponse::fromEntity).toList();
	}

	//등록·수정: 그 날짜 기록이 있으면 값을 바꾸고, 없으면 새로 만든다. 미래 날짜는 안 된다.
	@Transactional
	public WeightResponse saveWeightRecord(Long userId, LocalDate date, WeightRequest request) {
		if (date.isAfter(LocalDate.now(clock))) {
			throw new BusinessException(ErrorCode.FUTURE_DATE_NOT_ALLOWED);
		}
		Optional<WeightRecord> existing = weightRecordRepository.findByUserIdAndRecordDate(userId, date);
		if (existing.isPresent()) {
			WeightRecord weightRecord = existing.get();
			weightRecord.updateWeight(request.weight());
			return WeightResponse.fromEntity(weightRecord);
		}
		User user = userRepository.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
		WeightRecord weightRecord = weightRecordRepository.save(WeightRecord.create(user, date, request.weight()));
		return WeightResponse.fromEntity(weightRecord);
	}

	//삭제
	@Transactional
	public void deleteWeightRecord(Long userId, LocalDate date) {
		WeightRecord weightRecord = weightRecordRepository.findByUserIdAndRecordDate(userId, date)
				.orElseThrow(() -> new BusinessException(ErrorCode.WEIGHT_RECORD_NOT_FOUND));
		weightRecordRepository.delete(weightRecord);
	}
}
