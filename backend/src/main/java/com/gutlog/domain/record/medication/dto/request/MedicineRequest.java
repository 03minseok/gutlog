package com.gutlog.domain.record.medication.dto.request;

import com.gutlog.domain.record.medication.entity.DosageForm;
import com.gutlog.domain.record.medication.entity.MedicineCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 관리자 약 추가·수정 요청 */
public record MedicineRequest(
		@NotBlank(message = "성분명은 필수입니다.")
		@Size(max = 50, message = "성분명은 50자 이하로 입력해주세요.")
		String genericName,

		@NotBlank(message = "제품명은 필수입니다.")
		@Size(max = 50, message = "제품명은 50자 이하로 입력해주세요.")
		String brandName,

		@NotNull(message = "제형은 필수입니다.")
		DosageForm dosageForm,

		@NotNull(message = "분류는 필수입니다.")
		MedicineCategory category
) {
}
