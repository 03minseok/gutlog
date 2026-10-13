package com.gutlog.domain.record.medication.service;

import com.gutlog.domain.record.medication.dto.request.UserMedicineCreateRequest;
import com.gutlog.domain.record.medication.dto.request.UserMedicineUpdateRequest;
import com.gutlog.domain.record.medication.dto.response.UserMedicineResponse;
import com.gutlog.domain.record.medication.entity.UserMedicine;
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
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserMedicineService {
	private final UserMedicineRepository userMedicineRepository;
	private final MedicineRepository medicineRepository;
	private final UserRepository userRepository;
	private final Clock clock;

	//내 복용 약 목록 (끊지 않은 약)
	public List<UserMedicineResponse> getMyMedicines(Long userId) {
		return userMedicineRepository.findActiveWithMedicine(userId).stream()
				.map(UserMedicineResponse::fromEntity)
				.toList();
	}

	//등록
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

		UserMedicine userMedicine = UserMedicine.create(user, medicine, request.scheduleType(), request.dailyDoseCount(),
				request.intervalDays(), request.doseAmount(), request.doseMemo());
		userMedicineRepository.save(userMedicine);
		return UserMedicineResponse.fromEntity(userMedicine);
	}

	//수정
	@Transactional
	public UserMedicineResponse editUserMedicine(Long userId, Long userMedicineId, UserMedicineUpdateRequest request) {
		UserMedicine userMedicine = findMyActiveMedicine(userId, userMedicineId);
		validateDoseAmount(userMedicine.getMedicine(), request.doseAmount());
		userMedicine.update(request.scheduleType(), request.dailyDoseCount(), request.intervalDays(),
				request.doseAmount(), request.doseMemo());
		return UserMedicineResponse.fromEntity(userMedicine);
	}

	//삭제 (소프트). 과거 복용 기록은 그대로 남는다.
	@Transactional
	public void deleteUserMedicine(Long userId, Long userMedicineId) {
		UserMedicine userMedicine = findMyActiveMedicine(userId, userMedicineId);
		userMedicine.delete(LocalDateTime.now(clock));
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

	// 복용량 간격·범위는 제형마다 다르다. (정제 0.5정 단위, 내복액 0.1mL 단위)
	private static void validateDoseAmount(Medicine medicine, BigDecimal doseAmount) {
		if (doseAmount != null && !medicine.getDosageForm().isValidDoseAmount(doseAmount)) {
			throw new BusinessException(ErrorCode.INVALID_DOSE_AMOUNT);
		}
	}
}
