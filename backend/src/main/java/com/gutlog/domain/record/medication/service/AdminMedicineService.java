package com.gutlog.domain.record.medication.service;

import com.gutlog.domain.record.medication.dto.request.MedicineRequest;
import com.gutlog.domain.record.medication.dto.response.MedicineResponse;
import com.gutlog.domain.record.medication.entity.Medicine;
import com.gutlog.domain.record.medication.repository.MedicineRepository;
import com.gutlog.global.exception.BusinessException;
import com.gutlog.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminMedicineService {
	private final MedicineRepository medicineRepository;

	//전체조회 (비활성 포함)
	public List<MedicineResponse> getAllMedicines() {
		return medicineRepository.findAllOrdered().stream().map(MedicineResponse::fromEntity).toList();
	}

	//추가
	@Transactional
	public MedicineResponse createMedicine(MedicineRequest request) {
		validateNotDuplicate(request, null);
		Medicine medicine = Medicine.create(request.genericName(), request.brandName(), request.dosageForm(), request.category());
		medicineRepository.save(medicine);
		return MedicineResponse.fromEntity(medicine);
	}

	//수정
	@Transactional
	public MedicineResponse editMedicine(Long medicineId, MedicineRequest request) {
		Medicine medicine = findMedicine(medicineId);
		validateNotDuplicate(request, medicineId);
		medicine.update(request.genericName(), request.brandName(), request.dosageForm(), request.category());
		return MedicineResponse.fromEntity(medicine);
	}

	//비활성화
	@Transactional
	public MedicineResponse deactivateMedicine(Long medicineId) {
		Medicine medicine = findMedicine(medicineId);
		medicine.deactivate();
		return MedicineResponse.fromEntity(medicine);
	}

	//재활성화
	@Transactional
	public MedicineResponse activateMedicine(Long medicineId) {
		Medicine medicine = findMedicine(medicineId);
		medicine.activate();
		return MedicineResponse.fromEntity(medicine);
	}

	private Medicine findMedicine(Long medicineId) {
		return medicineRepository.findById(medicineId).orElseThrow(() -> new BusinessException(ErrorCode.MEDICINE_NOT_FOUND));
	}

	// 같은 성분명 + 제품명 + 제형은 하나만 (수정할 때 자기 자신은 제외)
	private void validateNotDuplicate(MedicineRequest request, Long selfId) {
		medicineRepository.findByGenericNameAndBrandNameAndDosageForm(
						request.genericName().strip(), request.brandName().strip(), request.dosageForm())
				.filter(found -> !found.getId().equals(selfId))
				.ifPresent(found -> {
					throw new BusinessException(ErrorCode.DUPLICATE_MEDICINE);
				});
	}
}
