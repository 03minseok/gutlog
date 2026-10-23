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
 * 복용 방식과 복용량은 MedicationSchedule에 기간별로 둔다. 이 엔티티는 "누가 어떤 약을 먹는가"만 가진다.
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

	@Column(nullable = false)
	private boolean deleted;

	/** 끊은 시각. 과거 날짜 화면에서 "그날 복용 중이던 약"을 고를 때 쓴다. */
	private LocalDateTime deletedAt;

	private UserMedicine(User user, Medicine medicine) {
		this.user = user;
		this.medicine = medicine;
	}

	public static UserMedicine create(User user, Medicine medicine) {
		return new UserMedicine(user, medicine);
	}

	public void delete(LocalDateTime now) {
		this.deleted = true;
		this.deletedAt = now;
	}

	public boolean isOwnedBy(Long userId) {
		return this.user.getId().equals(userId);
	}
}
