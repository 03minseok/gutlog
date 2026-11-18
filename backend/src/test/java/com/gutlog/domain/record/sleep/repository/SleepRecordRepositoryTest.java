package com.gutlog.domain.record.sleep.repository;

import com.gutlog.domain.record.sleep.entity.SleepRecord;
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

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * sleep 레포지토리 슬라이스 테스트.
 * 기간 경계(양쪽 포함), 정렬, 유저 필터, 날짜당 한 줄 유니크 조건을 실제 H2에서 확인한다.
 */
@DataJpaTest
class SleepRecordRepositoryTest {

    @Autowired UserRepository userRepository;
    @Autowired SleepRecordRepository sleepRecordRepository;

    private User me;
    private User other;

    @BeforeEach
    void setUp() {
        me = userRepository.save(User.create("me", "ENCODED", "나", Role.PATIENT, IbdType.CROHNS_DISEASE));
        other = userRepository.save(User.create("other", "ENCODED", "남", Role.GUARDIAN, null));
    }

    private SleepRecord save(User user, LocalDate date, int minutes) {
        return sleepRecordRepository.save(SleepRecord.create(user, date, minutes));
    }

    @Test
    @DisplayName("기간 조회 - from, to를 모두 포함하고 날짜순으로 정렬한다")
    void findByPeriod() {
        LocalDate from = LocalDate.of(2026, 10, 1);
        LocalDate to = LocalDate.of(2026, 10, 7);

        SleepRecord middle = save(me, LocalDate.of(2026, 10, 4), 420);
        SleepRecord atTo = save(me, to, 480);                     // 포함
        SleepRecord atFrom = save(me, from, 390);                 // 포함
        save(me, from.minusDays(1), 300);                         // 제외 (전날)
        save(me, to.plusDays(1), 360);                            // 제외 (다음날)
        save(other, LocalDate.of(2026, 10, 4), 600);              // 제외 (남의 기록)

        List<SleepRecord> result = sleepRecordRepository
                .findAllByUserIdAndRecordDateBetweenOrderByRecordDateAsc(me.getId(), from, to);

        assertThat(result).extracting(SleepRecord::getId)
                .containsExactly(atFrom.getId(), middle.getId(), atTo.getId());
    }

    @Test
    @DisplayName("날짜로 조회 - 내 기록만 찾는다")
    void findByUserAndDate() {
        LocalDate date = LocalDate.of(2026, 10, 4);
        save(other, date, 600);

        assertThat(sleepRecordRepository.findByUserIdAndRecordDate(me.getId(), date)).isEmpty();

        SleepRecord mine = save(me, date, 420);
        assertThat(sleepRecordRepository.findByUserIdAndRecordDate(me.getId(), date))
                .get().extracting(SleepRecord::getId).isEqualTo(mine.getId());
    }

    @Test
    @DisplayName("분 값을 그대로 저장한다")
    void keepsMinutes() {
        SleepRecord saved = save(me, LocalDate.of(2026, 10, 4), 1440);
        sleepRecordRepository.flush();

        SleepRecord found = sleepRecordRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getSleepMinutes()).isEqualTo(1440);
    }

    @Test
    @DisplayName("유니크 조건 - 같은 유저·같은 날짜는 두 줄을 넣을 수 없다")
    void uniquePerUserAndDate() {
        LocalDate date = LocalDate.of(2026, 10, 4);
        save(me, date, 420);
        save(other, date, 600);   // 다른 유저는 같은 날짜여도 된다
        sleepRecordRepository.flush();

        assertThatThrownBy(() -> sleepRecordRepository.saveAndFlush(SleepRecord.create(me, date, 480)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
