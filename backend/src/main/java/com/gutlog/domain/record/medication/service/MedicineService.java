package com.gutlog.domain.record.medication.service;

import com.gutlog.domain.record.medication.dto.response.MedicineResponse;
import com.gutlog.domain.record.medication.entity.Medicine;
import com.gutlog.domain.record.medication.repository.MedicineRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MedicineService {
	private final MedicineRepository medicineRepository;

	//검색 (활성 약만). keyword가 없으면 활성 약 전체
	public List<MedicineResponse> searchMedicines(String keyword) {
		String trimmed = keyword == null ? "" : keyword.strip();
		List<Medicine> medicines = trimmed.isEmpty()
				? medicineRepository.findAllActive()
				: medicineRepository.searchActive(trimmed);
		return medicines.stream().map(MedicineResponse::fromEntity).toList();
	}
}
