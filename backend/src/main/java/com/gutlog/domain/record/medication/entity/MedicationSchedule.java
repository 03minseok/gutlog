package com.gutlog.domain.record.medication.entity;

import com.gutlog.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(indexes = @Index(columnList = "user_medicine_id, effective_from"))
public class MedicationSchedule extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_medicine_id", nullable = false)
    private UserMedicine userMedicine;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ScheduleType scheduleType;

    private Integer dailyDoseCount;
    private Integer intervalDays;

    @Column(precision = 4, scale = 1)
    private BigDecimal doseAmount;

    @Column(length = 100)
    private String doseMemo;

    @Column(nullable = false)
    private LocalDate effectiveFrom;

    private LocalDate effectiveTo;

    private MedicationSchedule(UserMedicine userMedicine, LocalDate effectiveFrom, ScheduleType scheduleType,
                               Integer dailyDoseCount, Integer intervalDays, BigDecimal doseAmount, String doseMemo) {
        this.userMedicine = userMedicine;
        this.effectiveFrom = effectiveFrom;
        apply(scheduleType, dailyDoseCount, intervalDays, doseAmount, doseMemo);
    }

    public static MedicationSchedule create(UserMedicine userMedicine, LocalDate effectiveFrom, ScheduleType scheduleType,
                                            Integer dailyDoseCount, Integer intervalDays, BigDecimal doseAmount, String doseMemo) {
        return new MedicationSchedule(userMedicine, effectiveFrom, scheduleType, dailyDoseCount, intervalDays, doseAmount, doseMemo);
    }

    /** 오늘 시작한 일정만 고친다. 그 외에는 close 후 새 일정을 만든다. */
    public void update(ScheduleType scheduleType, Integer dailyDoseCount, Integer intervalDays, BigDecimal doseAmount, String doseMemo) {
        apply(scheduleType, dailyDoseCount, intervalDays, doseAmount, doseMemo);
    }

    public void close(LocalDate date) {
        this.effectiveTo = date;
    }

    public boolean startedOn(LocalDate date) {
        return this.effectiveFrom.equals(date);
    }

    /** 체크할 수 있는 가장 큰 회차. DAILY는 하루 복용 횟수, 나머지는 1 */
    public int maxDoseNumber() {
        return scheduleType == ScheduleType.DAILY ? dailyDoseCount : 1;
    }

    private void apply(ScheduleType scheduleType, Integer dailyDoseCount, Integer intervalDays, BigDecimal doseAmount, String doseMemo) {
        this.scheduleType = scheduleType;
        this.dailyDoseCount = scheduleType.usesDailyDoseCount() ? dailyDoseCount : null;
        this.intervalDays = scheduleType.usesIntervalDays() ? intervalDays : null;
        this.doseAmount = doseAmount;
        this.doseMemo = doseMemo == null || doseMemo.isBlank() ? null : doseMemo.strip();
    }
}
