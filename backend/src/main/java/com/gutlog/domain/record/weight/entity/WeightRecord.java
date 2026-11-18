package com.gutlog.domain.record.weight.entity;

import com.gutlog.domain.user.entity.User;
import com.gutlog.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 체중 기록: "이 날짜의 체중은 n kg". 날짜당 한 줄뿐이며, 같은 날짜로 다시 등록하면 값을 바꾼다.
 */
@Entity
@Getter
@Table(name = "weight_record",
		uniqueConstraints = @UniqueConstraint(name = "uk_weight_record_user_date", columnNames = {"user_id", "record_date"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WeightRecord extends BaseTimeEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(name = "record_date", nullable = false)
	private LocalDate recordDate;

	@Column(nullable = false, precision = 5, scale = 2)
	private BigDecimal weight;

	private WeightRecord(User user, LocalDate recordDate, BigDecimal weight) {
		this.user = user;
		this.recordDate = recordDate;
		this.weight = weight;
	}

	public static WeightRecord create(User user, LocalDate recordDate, BigDecimal weight) {
		return new WeightRecord(user, recordDate, weight);
	}

	public void updateWeight(BigDecimal weight) {
		this.weight = weight;
	}
}
