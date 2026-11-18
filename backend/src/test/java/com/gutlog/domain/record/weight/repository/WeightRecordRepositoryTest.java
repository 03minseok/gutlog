package com.gutlog.domain.record.weight.repository;

import com.gutlog.domain.record.weight.entity.WeightRecord;
import com.gutlog.domain.user.entity.IbdType;
import com.gutlog.domain.user.entity.Role;
import com.gutlog.domain.user.entity.User;
import com.gutlog.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * weight 레포지토리 슬라이스 테스트.
 * 기간 경계(양쪽 포함), 정렬, 유저 필터, 날짜당 한 줄 유니크 조건을 실제 H2에서 확인한다.
 */
@DataJpaTest
class WeightRecordRepositoryTest {

    @Autowired UserRepository userRepository;
    @Autowired WeightRecordRepository weightRecordRepository;

    private User me;
    private User other;

    @BeforeEach
    void setUp() {
        me = userRepository.save(User.create("me", "ENCODED", "나", Role.PATIENT, IbdType.CROHNS_DISEASE));
        other = userRepository.save(User.create("other", "ENCODED", "남", Role.GUARDIAN, null));
    }

    private WeightRecord save(User user, LocalDate date, String weight) {
        return weightRecordRepository.save(WeightRecord.create(user, date, new BigDecimal(weight)));
    }

    @Test
    @DisplayName("기간 조회 - from, to를 모두 포함하고 날짜순으로 정렬한다")
    void findByPeriod() {
        LocalDate from = LocalDate.of(2026, 10, 1);
        LocalDate to = LocalDate.of(2026, 10, 7);

        WeightRecord middle = save(me, LocalDate.of(2026, 10, 4), "60.10");
        WeightRecord atTo = save(me, to, "60.30");                   // 포함
        WeightRecord atFrom = save(me, from, "60.00");               // 포함
        save(me, from.minusDays(1), "59.90");                        // 제외 (전날)
        save(me, to.plusDays(1), "60.40");                           // 제외 (다음날)
        save(other, LocalDate.of(2026, 10, 4), "80.00");             // 제외 (남의 기록)

        List<WeightRecord> result = weightRecordRepository
                .findAllByUserIdAndRecordDateBetweenOrderByRecordDateAsc(me.getId(), from, to);

        assertThat(result).extracting(WeightRecord::getId)
                .containsExactly(atFrom.getId(), middle.getId(), atTo.getId());
    }

    @Test
    @DisplayName("날짜로 조회 - 내 기록만 찾는다")
    void findByUserAndDate() {
        LocalDate date = LocalDate.of(2026, 10, 4);
        save(other, date, "80.00");

        assertThat(weightRecordRepository.findByUserIdAndRecordDate(me.getId(), date)).isEmpty();

        WeightRecord mine = save(me, date, "60.10");
        assertThat(weightRecordRepository.findByUserIdAndRecordDate(me.getId(), date))
                .get().extracting(WeightRecord::getId).isEqualTo(mine.getId());
    }

    @Test
    @DisplayName("소수점 두 자리 값을 그대로 저장한다")
    void keepsTwoDecimalPlaces() {
        WeightRecord saved = save(me, LocalDate.of(2026, 10, 4), "62.35");
        weightRecordRepository.flush();

        WeightRecord found = weightRecordRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getWeight()).isEqualByComparingTo("62.35");
    }

    @Test
    @DisplayName("유니크 조건 - 같은 유저·같은 날짜는 두 줄을 넣을 수 없다")
    void uniquePerUserAndDate() {
        LocalDate date = LocalDate.of(2026, 10, 4);
        save(me, date, "60.00");
        save(other, date, "80.00");   // 다른 유저는 같은 날짜여도 된다
        weightRecordRepository.flush();

        assertThatThrownBy(() -> weightRecordRepository.saveAndFlush(
                WeightRecord.create(me, date, new BigDecimal("61.00"))))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
