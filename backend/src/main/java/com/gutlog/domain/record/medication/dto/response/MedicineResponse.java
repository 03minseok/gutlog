package com.gutlog.domain.record.medication.dto.response;

import com.gutlog.domain.record.medication.entity.DosageForm;
import com.gutlog.domain.record.medication.entity.Medicine;
import com.gutlog.domain.record.medication.entity.MedicineCategory;

/** doseUnit: 복용량 단위 (정, 캡슐, 포, mL, 개, 회) */
public record MedicineResponse(
		Long id,
		String genericName,
		String brandName,
		DosageForm dosageForm,
		String doseUnit,
		MedicineCategory category,
		boolean active
) {
	public static MedicineResponse fromEntity(Medicine medicine) {
		return new MedicineResponse(
				medicine.getId(),
				medicine.getGenericName(),
				medicine.getBrandName(),
				medicine.getDosageForm(),
				medicine.getDosageForm().getDoseUnit(),
				medicine.getCategory(),
				medicine.isActive()
		);
	}
}
