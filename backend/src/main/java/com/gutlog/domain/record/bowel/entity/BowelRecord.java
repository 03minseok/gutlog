package com.gutlog.domain.record.bowel.entity;

import com.gutlog.domain.user.entity.User;
import com.gutlog.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name="bowel_record",
indexes = @Index(name="idx_bowel_record_user_bowelAt", columnList = "user_id, bowel_at"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BowelRecord extends BaseTimeEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(nullable = false)
	private LocalDateTime bowelAt;

	@Column(nullable = false)
	private Boolean bleeding;

	@Column(nullable = false)
	@Enumerated(EnumType.STRING)
	private BristolType bowelCondition;

	@Column(nullable = false)
	private Integer painLevel;

	@Column(length = 500)
	private String memo;

	private BowelRecord(User user, LocalDateTime bowelAt, Boolean bleeding, BristolType bowelCondition, Integer painLevel, String memo) {
		this.user = user;
		this.bowelAt = bowelAt;
		this.bleeding = bleeding;
		this.bowelCondition = bowelCondition;
		this.painLevel = painLevel;
		this.memo = memo;
	}

	public static BowelRecord create(User user, LocalDateTime bowelAt, Boolean bleeding, BristolType bowelCondition, Integer painLevel, String memo) {
		return new BowelRecord(user, bowelAt, bleeding, bowelCondition, painLevel, memo);
	}

	public void update(LocalDateTime bowelAt, Boolean bleeding, BristolType bowelCondition, Integer painLevel, String memo) {
		this.bowelAt = bowelAt;
		this.bleeding = bleeding;
		this.bowelCondition = bowelCondition;
		this.painLevel = painLevel;
		this.memo = memo;
	}

	public boolean isOwnedBy(Long userId) {
		return this.user.getId().equals(userId);
	}
}
