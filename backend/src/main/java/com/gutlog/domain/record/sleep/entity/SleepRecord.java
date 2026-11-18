package com.gutlog.domain.record.sleep.entity;

import com.gutlog.domain.user.entity.User;
import com.gutlog.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 수면 기록: "이 날짜에 일어나기까지 n분 잤다". 낮잠은 제외한 총 수면 시간이다.
 * 일어난 날짜로 기록하며(15일 밤에 자서 16일에 일어나면 16일), 날짜당 한 줄뿐이다. 같은 날짜로 다시 등록하면 값을 바꾼다.
 */
@Entity
@Getter
@Table(name = "sleep_record",
		uniqueConstraints = @UniqueConstraint(name = "uk_sleep_record_user_date", columnNames = {"user_id", "record_date"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SleepRecord extends BaseTimeEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(name = "record_date", nullable = false)
	private LocalDate recordDate;

	@Column(name = "sleep_minutes", nullable = false)
	private Integer sleepMinutes;

	private SleepRecord(User user, LocalDate recordDate, Integer sleepMinutes) {
		this.user = user;
		this.recordDate = recordDate;
		this.sleepMinutes = sleepMinutes;
	}

	public static SleepRecord create(User user, LocalDate recordDate, Integer sleepMinutes) {
		return new SleepRecord(user, recordDate, sleepMinutes);
	}

	public void updateSleepMinutes(Integer sleepMinutes) {
		this.sleepMinutes = sleepMinutes;
	}
}
