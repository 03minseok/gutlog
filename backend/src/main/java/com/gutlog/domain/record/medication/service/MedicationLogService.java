package com.gutlog.domain.record.medication.service;

import com.gutlog.domain.record.medication.dto.response.DailyMedicationResponse;
import com.gutlog.domain.record.medication.dto.response.MedicationLogResponse;
import com.gutlog.domain.record.medication.dto.response.MedicationToggleResponse;
import com.gutlog.domain.record.medication.entity.MedicationLog;
import com.gutlog.domain.record.medication.entity.ScheduleType;
import com.gutlog.domain.record.medication.entity.UserMedicine;
import com.gutlog.domain.record.medication.repository.LastTakenDate;
import com.gutlog.domain.record.medication.repository.MedicationLogRepository;
import com.gutlog.domain.record.medication.repository.UserMedicineRepository;
import com.gutlog.global.exception.BusinessException;
import com.gutlog.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MedicationLogService {
	private final MedicationLogRepository medicationLogRepository;
	private final UserMedicineRepository userMedicineRepository;
	private final Clock clock;

	//날짜별 조회: 그날 복용 중이던 약 + 그날 기록이 있는 약(끊은 약 포함), 각 약의 체크한 회차
	public DailyMedicationResponse getDailyMedication(Long userId, LocalDate date) {
		List<UserMedicine> inUse = userMedicineRepository.findInUseOn(userId, date.plusDays(1).atStartOfDay());
		List<MedicationLog> logs = medicationLogRepository.findTakenOn(userId, date);

		// 약 id 순서로 보여준다. 그날 복용 중이 아니던 약은 기록이 있을 때만 들어간다.
		Map<Long, UserMedicine> shown = new TreeMap<>();
		inUse.forEach(um -> shown.put(um.getId(), um));
		logs.forEach(log -> shown.putIfAbsent(log.getUserMedicine().getId(), log.getUserMedicine()));

		Map<Long, List<Integer>> dosesByMedicine = logs.stream().collect(Collectors.groupingBy(
				log -> log.getUserMedicine().getId(),
				Collectors.mapping(MedicationLog::getDoseNumber, Collectors.toList())));

		Map<Long, LocalDate> lastTakenByMedicine = findLastTakenDates(shown.values(), date);

		int takenCount = 0;
		int totalCount = 0;
		for (UserMedicine um : inUse) {
			if (um.getScheduleType() != ScheduleType.DAILY) continue;
			totalCount += um.getDailyDoseCount();
			takenCount += (int) dosesByMedicine.getOrDefault(um.getId(), List.of()).stream()
					.filter(dose -> dose <= um.getDailyDoseCount())
					.count();
		}

		List<DailyMedicationResponse.Item> items = shown.values().stream()
				.map(um -> DailyMedicationResponse.Item.of(
						um,
						dosesByMedicine.getOrDefault(um.getId(), List.of()),
						lastTakenByMedicine.get(um.getId())))
				.toList();
		return new DailyMedicationResponse(date, takenCount, totalCount, items);
	}

	//기간 조회 (from, to 둘 다 포함)
	public List<MedicationLogResponse> getMedicationLogsByPeriod(Long userId, LocalDate from, LocalDate to) {
		if (from.isAfter(to)) {
			throw new BusinessException(ErrorCode.INVALID_DATE_RANGE);
		}
		return medicationLogRepository.findTakenBetween(userId, from, to.plusDays(1)).stream()
				.map(MedicationLogResponse::fromEntity)
				.toList();
	}

	/**
	 * 체크 토글. 체크되어 있으면 취소하고, 아니면 체크한다.
	 * - 체크: n회차는 n-1회차가 체크되어 있어야 한다. 회차는 1 ~ 하루 복용 횟수(DAILY가 아니면 1)
	 * - 취소: n+1회차가 체크되어 있으면 취소할 수 없다.
	 * - 끊은 약, 미래 날짜는 체크도 취소도 할 수 없다.
	 */
	@Transactional
	public MedicationToggleResponse toggleMedicationLog(Long userId, Long userMedicineId, LocalDate takenDate, int doseNumber) {
		UserMedicine userMedicine = userMedicineRepository.findById(userMedicineId)
				.orElseThrow(() -> new BusinessException(ErrorCode.USER_MEDICINE_NOT_FOUND));
		if (!userMedicine.isOwnedBy(userId)) {
			throw new BusinessException(ErrorCode.USER_MEDICINE_NOT_FOUND);
		}
		if (userMedicine.isDeleted()) {
			throw new BusinessException(ErrorCode.STOPPED_MEDICINE);
		}
		if (takenDate.isAfter(LocalDate.now(clock))) {
			throw new BusinessException(ErrorCode.FUTURE_DATE_NOT_ALLOWED);
		}
		if (doseNumber < 1) {
			throw new BusinessException(ErrorCode.INVALID_DOSE_NUMBER);
		}

		Optional<MedicationLog> existing = medicationLogRepository
				.findByUserMedicineIdAndTakenDateAndDoseNumber(userMedicineId, takenDate, doseNumber);
		boolean taken = existing.map(log -> !log.isDeleted()).orElse(false);

		if (taken) {
			if (isTaken(userMedicineId, takenDate, doseNumber + 1)) {
				throw new BusinessException(ErrorCode.DOSE_ORDER_UNCHECK);
			}
			existing.get().delete();
			return new MedicationToggleResponse(userMedicineId, takenDate, doseNumber, false);
		}

		// 횟수를 줄여 범위 밖이 된 회차는 새로 체크할 수 없다. (이미 있는 기록은 취소할 수 있다)
		if (doseNumber > userMedicine.maxDoseNumber()) {
			throw new BusinessException(ErrorCode.INVALID_DOSE_NUMBER);
		}
		if (doseNumber > 1 && !isTaken(userMedicineId, takenDate, doseNumber - 1)) {
			throw new BusinessException(ErrorCode.DOSE_ORDER_CHECK);
		}
		// 취소했던 회차면 새로 넣지 않고 삭제 표시를 되돌린다. (유니크 제약)
		existing.ifPresentOrElse(
				MedicationLog::restore,
				() -> medicationLogRepository.save(MedicationLog.create(userMedicine, takenDate, doseNumber)));
		return new MedicationToggleResponse(userMedicineId, takenDate, doseNumber, true);
	}

	private boolean isTaken(Long userMedicineId, LocalDate takenDate, int doseNumber) {
		return medicationLogRepository.existsByUserMedicineIdAndTakenDateAndDoseNumberAndDeletedFalse(userMedicineId, takenDate, doseNumber);
	}

	// INTERVAL 약만 마지막 투여일을 한 번의 쿼리로 구한다.
	private Map<Long, LocalDate> findLastTakenDates(Collection<UserMedicine> medicines, LocalDate date) {
		List<Long> intervalIds = medicines.stream()
				.filter(um -> um.getScheduleType() == ScheduleType.INTERVAL)
				.map(UserMedicine::getId)
				.toList();
		if (intervalIds.isEmpty()) {
			return Map.of();
		}
		return medicationLogRepository.findLastTakenDates(intervalIds, date).stream()
				.collect(Collectors.toMap(LastTakenDate::userMedicineId, LastTakenDate::takenDate));
	}
}
