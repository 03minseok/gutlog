package com.gutlog.domain.record.medication.entity;

import com.gutlog.domain.user.entity.User;
import com.gutlog.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 사용자가 등록한 "내가 먹는 약".
 * 끊으면 소프트 딜리트한다. 과거 복용 기록이 이 행을 가리키므로 지우지 않는다.
 * @SQLRestriction을 붙이지 않는다: 붙이면 끊은 약을 JPA가 읽지 못해서, 남아 있는 복용 기록에서 약 정보를 꺼낼 때 에러가 난다.
 * 대신 "내 복용 약 목록" 같은 조회 쿼리에서 deleted = false를 직접 건다.
 */
@Entity
@Getter
@Table(name = "user_medicine",
		indexes = @Index(name = "idx_user_medicine_user_deleted", columnList = "user_id, deleted"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserMedicine extends BaseTimeEntity {
	public static final int DOSE_MEMO_MAX_LENGTH = 20;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "medicine_id", nullable = false)
	private Medicine medicine;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ScheduleType scheduleType;

	/** DAILY일 때만 (1~6) */
	private Integer dailyDoseCount;

	/** INTERVAL일 때만 (일 수) */
	private Integer intervalDays;

	/** 1회 복용량. 단위는 약의 제형이 정한다. (예: 1.5정) */
	@Column(precision = 4, scale = 1)
	private BigDecimal doseAmount;

	@Column(length = DOSE_MEMO_MAX_LENGTH)
	private String doseMemo;

	@Column(nullable = false)
	private boolean deleted;

	/** 끊은 시각. 과거 날짜 화면에서 "그날 복용 중이던 약"을 고를 때 쓴다. */
	private LocalDateTime deletedAt;

	private UserMedicine(User user, Medicine medicine, ScheduleType scheduleType, Integer dailyDoseCount,
						 Integer intervalDays, BigDecimal doseAmount, String doseMemo) {
		this.user = user;
		this.medicine = medicine;
		applySchedule(scheduleType, dailyDoseCount, intervalDays, doseAmount, doseMemo);
	}

	public static UserMedicine create(User user, Medicine medicine, ScheduleType scheduleType, Integer dailyDoseCount,
									  Integer intervalDays, BigDecimal doseAmount, String doseMemo) {
		return new UserMedicine(user, medicine, scheduleType, dailyDoseCount, intervalDays, doseAmount, doseMemo);
	}

	/** 복용 방식·복용량 수정. 약 자체는 바꿀 수 없다. 이미 남긴 복용 기록은 그대로 둔다. */
	public void update(ScheduleType scheduleType, Integer dailyDoseCount, Integer intervalDays, BigDecimal doseAmount, String doseMemo) {
		applySchedule(scheduleType, dailyDoseCount, intervalDays, doseAmount, doseMemo);
	}

	public void delete(LocalDateTime now) {
		this.deleted = true;
		this.deletedAt = now;
	}

	public boolean isOwnedBy(Long userId) {
		return this.user.getId().equals(userId);
	}

	/** 체크할 수 있는 가장 큰 회차. DAILY는 하루 복용 횟수, 나머지는 1 */
	public int maxDoseNumber() {
		return scheduleType == ScheduleType.DAILY ? dailyDoseCount : 1;
	}

	private void applySchedule(ScheduleType scheduleType, Integer dailyDoseCount, Integer intervalDays, BigDecimal doseAmount, String doseMemo) {
		this.scheduleType = scheduleType;
		// 복용 방식에 맞지 않는 값은 요청 검증에서 막는다. 엔티티에도 맞는 값만 남긴다.
		this.dailyDoseCount = scheduleType.usesDailyDoseCount() ? dailyDoseCount : null;
		this.intervalDays = scheduleType.usesIntervalDays() ? intervalDays : null;
		this.doseAmount = doseAmount;
		this.doseMemo = doseMemo == null || doseMemo.isBlank() ? null : doseMemo.strip();
	}
}
