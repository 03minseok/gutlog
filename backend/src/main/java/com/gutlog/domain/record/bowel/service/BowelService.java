package com.gutlog.domain.record.bowel.service;

import com.gutlog.domain.record.bowel.dto.request.BowelCreateRequest;
import com.gutlog.domain.record.bowel.dto.response.BowelResponse;
import com.gutlog.domain.record.bowel.entity.BowelRecord;
import com.gutlog.domain.record.bowel.repository.BowelRecordRepository;
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

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BowelService {
	private final BowelRecordRepository bowelRecordRepository;
	private final UserRepository userRepository;

	//조회
	public BowelResponse getBowelRecord(Long bowelRecordId, Long userId){
		BowelRecord bowelRecord = findMyBowel(bowelRecordId, userId);
		return BowelResponse.fromEntity(bowelRecord);
	}

	//전체조회
	public List<BowelResponse> getAllBowelRecordsByPeriod(Long userId, LocalDate from, LocalDate to){
		if(from.isAfter(to)){
			throw new BusinessException(ErrorCode.INVALID_DATE_RANGE);
		}
		LocalDateTime start = from.atStartOfDay();
		LocalDateTime end = to.plusDays(1).atStartOfDay();

		List<BowelRecord> bowels = bowelRecordRepository.
				findAllByUserIdAndBowelAtGreaterThanAndBowelAtLessThanOrderByBowelAtAsc(userId, start, end);
		if(bowels.isEmpty()){
			return List.of();
		}
		return bowels.stream().map(BowelResponse::fromEntity).toList();
	}

	//생성
	@Transactional
	public BowelResponse createBowelRecord(Long userId, BowelCreateRequest request){
		User user = userRepository.findById(userId).orElseThrow(()-> new BusinessException(ErrorCode.USER_NOT_FOUND));
		BowelRecord bowelRecord = request.toEntity(user);
		bowelRecordRepository.save(bowelRecord);
		return BowelResponse.fromEntity(bowelRecord);
	}

	//수정
	@Transactional
	public BowelResponse editBowelRecord(Long userId, Long bowelRecordId, BowelCreateRequest request){
		BowelRecord bowelRecord = findMyBowel(bowelRecordId, userId);
		bowelRecord.update(userId, request.bowelAt(), request.bleeding(), request.bowelCondition(), request.painLevel(), request.memo());
		return BowelResponse.fromEntity(bowelRecord);
	}

	//삭제
	@Transactional
	public void deleteBowelRecord(Long userId, Long bowelRecordId){
		BowelRecord bowelRecord = findMyBowel(bowelRecordId, userId);
		bowelRecordRepository.delete(bowelRecord);
	}

	private BowelRecord findMyBowel(Long bowelRecordId, Long userId){
		BowelRecord bowelRecord = bowelRecordRepository.findById(bowelRecordId).orElseThrow(()-> new BusinessException(ErrorCode.BOWEL_RECORD_NOT_FOUND));
		if (!bowelRecord.isOwnedBy(userId)) {
			throw new BusinessException(ErrorCode.BOWEL_RECORD_NOT_FOUND);
		}
		return bowelRecord;
	}
}
