package com.gutlog.domain.record.sleep.service;

import com.gutlog.domain.record.sleep.dto.request.SleepRequest;
import com.gutlog.domain.record.sleep.dto.response.SleepResponse;
import com.gutlog.domain.record.sleep.entity.SleepRecord;
import com.gutlog.domain.record.sleep.repository.SleepRecordRepository;
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
public class SleepService {
	private final SleepRecordRepository sleepRecordRepository;
	private final UserRepository userRepository;
	private final Clock clock;

	//기간 조회 (from, to 모두 포함, 날짜순)
	public List<SleepResponse> getSleepRecordsByPeriod(Long userId, LocalDate from, LocalDate to) {
		if (from.isAfter(to)) {
			throw new BusinessException(ErrorCode.INVALID_DATE_RANGE);
		}
		return sleepRecordRepository.findAllByUserIdAndRecordDateBetweenOrderByRecordDateAsc(userId, from, to)
				.stream().map(SleepResponse::fromEntity).toList();
	}

	//등록·수정: 그 날짜(일어난 날) 기록이 있으면 값을 바꾸고, 없으면 새로 만든다. 미래 날짜는 안 된다.
	@Transactional
	public SleepResponse saveSleepRecord(Long userId, LocalDate date, SleepRequest request) {
		if (date.isAfter(LocalDate.now(clock))) {
			throw new BusinessException(ErrorCode.FUTURE_DATE_NOT_ALLOWED);
		}
		Optional<SleepRecord> existing = sleepRecordRepository.findByUserIdAndRecordDate(userId, date);
		if (existing.isPresent()) {
			SleepRecord sleepRecord = existing.get();
			sleepRecord.updateSleepMinutes(request.sleepMinutes());
			return SleepResponse.fromEntity(sleepRecord);
		}
		User user = userRepository.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
		SleepRecord sleepRecord = sleepRecordRepository.save(SleepRecord.create(user, date, request.sleepMinutes()));
		return SleepResponse.fromEntity(sleepRecord);
	}

	//삭제
	@Transactional
	public void deleteSleepRecord(Long userId, LocalDate date) {
		SleepRecord sleepRecord = sleepRecordRepository.findByUserIdAndRecordDate(userId, date)
				.orElseThrow(() -> new BusinessException(ErrorCode.SLEEP_RECORD_NOT_FOUND));
		sleepRecordRepository.delete(sleepRecord);
	}
}
