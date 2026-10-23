package com.gutlog.domain.record.medication.entity;

import com.gutlog.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 복용 기록: "이 약을 이 날짜에 n회차 먹었다". 시각은 기록하지 않는다.
 * 체크 = 생성(또는 되돌리기), 체크 취소 = 소프트 딜리트.
 * 같은 약·날짜·회차는 한 행뿐이라, 취소했던 회차를 다시 체크하면 새로 넣지 않고 삭제 표시를 되돌린다.
 */
@Entity
@Getter
@Table(name = "medication_log",
		uniqueConstraints = @UniqueConstraint(name = "uk_medication_log_dose", columnNames = {"user_medicine_id", "taken_date", "dose_number"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MedicationLog extends BaseTimeEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_medicine_id", nullable = false)
	private UserMedicine userMedicine;

	@Column(nullable = false)
	private LocalDate takenDate;

	@Column(nullable = false)
	private int doseNumber;

	@Column(nullable = false)
	private boolean deleted;

	@Column(nullable = false)
	private Integer doseAmount; // 복용 당시 용량을 복사

	@Column(nullable = false, length = 10)
	private String doseUnit; // mg, 정, 포

	private MedicationLog(UserMedicine userMedicine, LocalDate takenDate, int doseNumber, Integer doseAmount, String doseUnit) {
		this.userMedicine = userMedicine;
		this.takenDate = takenDate;
		this.doseNumber = doseNumber;
		this.doseAmount = doseAmount;
		this.doseUnit = doseUnit;
	}

	public static MedicationLog create(UserMedicine userMedicine, LocalDate takenDate, int doseNumber, Integer doseAmount, String doseUnit) {
		return new MedicationLog(userMedicine, takenDate, doseNumber, doseAmount, doseUnit);
	}

	public void delete() {
		this.deleted = true;
	}

	public void restore() {
		this.deleted = false;
	}
}
