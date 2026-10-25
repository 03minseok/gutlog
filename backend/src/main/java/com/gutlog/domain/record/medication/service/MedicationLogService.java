package com.gutlog.domain.record.medication.service;

import com.gutlog.domain.record.medication.dto.response.DailyMedicationResponse;
import com.gutlog.domain.record.medication.dto.response.MedicationLogResponse;
import com.gutlog.domain.record.medication.dto.response.MedicationToggleResponse;
import com.gutlog.domain.record.medication.entity.MedicationLog;
import com.gutlog.domain.record.medication.entity.MedicationSchedule;
import com.gutlog.domain.record.medication.entity.ScheduleType;
import com.gutlog.domain.record.medication.entity.UserMedicine;
import com.gutlog.domain.record.medication.repository.LastTakenDate;
import com.gutlog.domain.record.medication.repository.MedicationLogRepository;
import com.gutlog.domain.record.medication.repository.MedicationScheduleRepository;
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
	private final MedicationScheduleRepository scheduleRepository;
	private final UserMedicineRepository userMedicineRepository;
	private final Clock clock;

	//날짜별 조회: 그날 일정이 있는 약 + 그날 기록이 있는 약(그날 끊은 약 포함)
	public DailyMedicationResponse getDailyMedication(Long userId, LocalDate date) {
		//그날 유효한 일정
		List<MedicationSchedule> inUse = scheduleRepository.findAllInEffectOn(userId, date);
		//그날 체크한 기록
		List<MedicationLog> logs = medicationLogRepository.findTakenOn(userId, date);

		// 화면에 보여줄 약을 모음. 키는 id, 값은 그 약의 일정.
		// 약 id 순서로 보여준다. 그날 복용 중이 아니던 약은 기록이 있을 때만, 그날 이전 마지막 일정으로 넣는다.
		Map<Long, MedicationSchedule> shown = new TreeMap<>();
		inUse.forEach(s -> shown.put(s.getUserMedicine().getId(), s));

		//기록만 있는 약을 찾아서 추가 -> 기록에 있는 약 id중 shown에 없는 것만 고름.
		// 예를 들어 오늘 끊은 약 B는 shown에는 포함되지 않지만 여기에서는 포함됨
		List<Long> logOnlyIds = logs.stream().
				map(log -> log.getUserMedicine().getId())
				.filter(id -> !shown.containsKey(id))
				.distinct()
				.toList();
		if(!logOnlyIds.isEmpty()) {
			scheduleRepository.findLatestStartedBy(logOnlyIds, date)
					.forEach(s -> shown.put(s.getUserMedicine().getId(), s));
		}

		//약별로 체크한 회차를 묶음
		Map<Long, List<Integer>> dosesByMedicine = logs.stream().collect(Collectors.groupingBy(
				log -> log.getUserMedicine().getId(),
				Collectors.mapping(MedicationLog::getDoseNumber, Collectors.toList())));

		//INTERVAL 약의 마지막 투여일을 구함.
		// 예를 들어 며칠 간격으로 먹는 약 C는 "마지막으로 언제 먹었나"를 보여줘야 다음 날짜를 알 수 있음.
		// 결과는  {C: 10월 21일}
		Map<Long, LocalDate> lastTakenByMedicine = findLastTakenDates(shown.values(), date);

		//오늘 복용률을 계산
		int takenCount = 0;
		int totalCount = 0;
		for (MedicationSchedule s : inUse) {
			if (s.getScheduleType() != ScheduleType.DAILY) continue;
			totalCount += s.getDailyDoseCount();
			takenCount += (int) dosesByMedicine.getOrDefault(s.getUserMedicine().getId(), List.of()).stream()
					.filter(dose -> dose <= s.getDailyDoseCount())
					.count();
		}

		List<DailyMedicationResponse.Item> items = shown.values().stream()
				.map(s -> DailyMedicationResponse.Item.of(
						s,
						dosesByMedicine.getOrDefault(s.getUserMedicine().getId(), List.of()),
						lastTakenByMedicine.get(s.getUserMedicine().getId())))
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

		// 회차 상한은 그날 일정이 정한다. 일정이 없는 날(등록전)은 체크할 수 없다.
		MedicationSchedule schedule = scheduleRepository.findInEffectOn(userMedicineId, takenDate)
				.orElseThrow(() -> new BusinessException(ErrorCode.NOT_IN_USE_ON_DATE));
		if (doseNumber > schedule.maxDoseNumber()) {
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
	private Map<Long, LocalDate> findLastTakenDates(Collection<MedicationSchedule> medicines, LocalDate date) {
		List<Long> intervalIds = medicines.stream()
				.filter(um -> um.getScheduleType() == ScheduleType.INTERVAL)
				.map(s -> s.getUserMedicine().getId())
				.toList();
		if (intervalIds.isEmpty()) {
			return Map.of();
		}
		return medicationLogRepository.findLastTakenDates(intervalIds, date).stream()
				.collect(Collectors.toMap(LastTakenDate::userMedicineId, LastTakenDate::takenDate));
	}
}
