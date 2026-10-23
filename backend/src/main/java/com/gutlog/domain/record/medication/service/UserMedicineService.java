package com.gutlog.domain.record.medication.service;

import com.gutlog.domain.record.medication.dto.request.UserMedicineCreateRequest;
import com.gutlog.domain.record.medication.dto.request.UserMedicineUpdateRequest;
import com.gutlog.domain.record.medication.dto.response.UserMedicineResponse;
import com.gutlog.domain.record.medication.entity.MedicationSchedule;
import com.gutlog.domain.record.medication.entity.UserMedicine;
import com.gutlog.domain.record.medication.repository.MedicationScheduleRepository;
import com.gutlog.domain.record.medication.repository.UserMedicineRepository;
import com.gutlog.domain.record.medication.entity.Medicine;
import com.gutlog.domain.record.medication.repository.MedicineRepository;
import com.gutlog.domain.user.entity.User;
import com.gutlog.domain.user.repository.UserRepository;
import com.gutlog.global.exception.BusinessException;
import com.gutlog.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserMedicineService {
	private final UserMedicineRepository userMedicineRepository;
	private final MedicationScheduleRepository scheduleRepository;
	private final MedicineRepository medicineRepository;
	private final UserRepository userRepository;
	private final Clock clock;

	//내 복용 약 목록 (끊지 않은 약)
	public List<UserMedicineResponse> getMyMedicines(Long userId) {
		return scheduleRepository.findCurrentByUserId(userId).stream()
				.map(UserMedicineResponse::fromEntity)
				.toList();
	}

	//등록: 약과 첫 일정을 함께
	@Transactional
	public UserMedicineResponse createUserMedicine(Long userId, UserMedicineCreateRequest request) {
		User user = userRepository.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
		// 비활성화된 약은 새로 등록할 수 없다.
		Medicine medicine = medicineRepository.findByIdAndActiveTrue(request.medicineId())
				.orElseThrow(() -> new BusinessException(ErrorCode.MEDICINE_NOT_FOUND));
		// 같은 약을 활성 상태로 두 번 등록할 수 없다. 끊었던 약은 새로 등록할 수 있다.
		if (userMedicineRepository.existsByUserIdAndMedicineIdAndDeletedFalse(userId, medicine.getId())) {
			throw new BusinessException(ErrorCode.DUPLICATE_USER_MEDICINE);
		}
		validateDoseAmount(medicine, request.doseAmount());
		UserMedicine userMedicine = userMedicineRepository.save(UserMedicine.create(user, medicine));
		MedicationSchedule schedule = scheduleRepository.save(MedicationSchedule.create(
				userMedicine, LocalDate.now(clock), request.scheduleType(), request.dailyDoseCount(),
				request.intervalDays(), request.doseAmount(), request.doseMemo()));
		return UserMedicineResponse.fromEntity(schedule);
	}

	//수정: 오늘 시작한 일정이면 덮어쓰고, 아니면 닫고 새로 만든다.
	@Transactional
	public UserMedicineResponse editUserMedicine(Long userId, Long userMedicineId, UserMedicineUpdateRequest request) {
		UserMedicine userMedicine = findMyActiveMedicine(userId, userMedicineId);
		validateDoseAmount(userMedicine.getMedicine(), request.doseAmount());

		LocalDate today = LocalDate.now(clock);
		MedicationSchedule current = findCurrentSchedule(userMedicineId);

		if(current.startedOn(today)){
			current.update(request.scheduleType(), request.dailyDoseCount(), request.intervalDays(),  request.doseAmount(), request.doseMemo());
			return UserMedicineResponse.fromEntity(current);
		}
		current.close(today);
		scheduleRepository.flush(); // 닫는 UPDATE를 새 일정 INSERT보다 먼저 보낸다 (현재 일정 유니크)
		MedicationSchedule next = scheduleRepository.save(MedicationSchedule.create(
				userMedicine, today, request.scheduleType(), request.dailyDoseCount(),
				request.intervalDays(), request.doseAmount(), request.doseMemo()
		));
		return UserMedicineResponse.fromEntity(next);
	}

	//삭제 (소프트). 현재 일정도 닫는다. 끊은 날은 복용 중에서 빠진다.
	@Transactional
	public void deleteUserMedicine(Long userId, Long userMedicineId) {
		UserMedicine userMedicine = findMyActiveMedicine(userId, userMedicineId);
		LocalDateTime now = LocalDateTime.now(clock);
		userMedicine.delete(now);
		findCurrentSchedule(userMedicineId).close(now.toLocalDate());
	}

	// 남의 약·끊은 약은 목록에 없는 약이므로 404
	private UserMedicine findMyActiveMedicine(Long userId, Long userMedicineId) {
		UserMedicine userMedicine = userMedicineRepository.findById(userMedicineId)
				.orElseThrow(() -> new BusinessException(ErrorCode.USER_MEDICINE_NOT_FOUND));
		if (!userMedicine.isOwnedBy(userId) || userMedicine.isDeleted()) {
			throw new BusinessException(ErrorCode.USER_MEDICINE_NOT_FOUND);
		}
		return userMedicine;
	}

	private MedicationSchedule findCurrentSchedule(Long userMedicineId) {
		return scheduleRepository.findCurrent(userMedicineId)
				.orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_NOT_FOUND));
	}

	// 복용량 간격·범위는 제형마다 다르다. (정제 0.5정 단위, 내복액 0.1mL 단위)
	private static void validateDoseAmount(Medicine medicine, BigDecimal doseAmount) {
		if (doseAmount != null && !medicine.getDosageForm().isValidDoseAmount(doseAmount)) {
			throw new BusinessException(ErrorCode.INVALID_DOSE_AMOUNT);
		}
	}
}
