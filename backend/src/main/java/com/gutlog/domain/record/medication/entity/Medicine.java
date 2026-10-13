package com.gutlog.domain.record.medication.entity;

import com.gutlog.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 약 마스터. 제품 + 제형마다 한 행이다. (예: 펜타사 서방정, 펜타사 좌약)
 * 사용자가 등록한 복용 약이 가리키고 있으므로 지우지 않고 비활성화한다.
 * 비활성 약은 검색·선택 목록에서 빠지지만, 이미 등록한 사용자에게는 그대로 보인다.
 */
@Entity
@Getter
@Table(name = "medicine",
		uniqueConstraints = @UniqueConstraint(name = "uk_medicine_name_form", columnNames = {"generic_name", "brand_name", "dosage_form"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Medicine extends BaseTimeEntity {
	public static final int NAME_MAX_LENGTH = 50;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** 성분명(일반명) */
	@Column(nullable = false, length = NAME_MAX_LENGTH)
	private String genericName;

	/** 대표 제품명(국내) */
	@Column(nullable = false, length = NAME_MAX_LENGTH)
	private String brandName;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private DosageForm dosageForm;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private MedicineCategory category;

	@Column(nullable = false)
	private boolean active;

	private Medicine(String genericName, String brandName, DosageForm dosageForm, MedicineCategory category) {
		this.genericName = genericName.strip();
		this.brandName = brandName.strip();
		this.dosageForm = dosageForm;
		this.category = category;
		this.active = true;
	}

	public static Medicine create(String genericName, String brandName, DosageForm dosageForm, MedicineCategory category) {
		return new Medicine(genericName, brandName, dosageForm, category);
	}

	public void update(String genericName, String brandName, DosageForm dosageForm, MedicineCategory category) {
		this.genericName = genericName.strip();
		this.brandName = brandName.strip();
		this.dosageForm = dosageForm;
		this.category = category;
	}

	public void activate() {
		this.active = true;
	}

	public void deactivate() {
		this.active = false;
	}
}
