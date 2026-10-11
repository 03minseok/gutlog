package com.gutlog.domain.record.bowel.repository;

import com.gutlog.domain.record.bowel.entity.BowelRecord;
import com.gutlog.domain.record.bowel.entity.BristolType;
import com.gutlog.domain.user.entity.IbdType;
import com.gutlog.domain.user.entity.Role;
import com.gutlog.domain.user.entity.User;
import com.gutlog.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * bowel 레포지토리 슬라이스 테스트.
 * 쿼리 메서드 이름으로 만든 조건(기간 경계, 정렬, 유저 필터)을 실제 H2에서 확인한다.
 */
@DataJpaTest
class BowelRecordRepositoryTest {

    @Autowired UserRepository userRepository;
    @Autowired BowelRecordRepository bowelRecordRepository;

    private User me;
    private User other;

    @BeforeEach
    void setUp() {
        me = userRepository.save(User.create("me", "ENCODED", "나", Role.PATIENT, IbdType.CROHNS_DISEASE));
        other = userRepository.save(User.create("other", "ENCODED", "남", Role.GUARDIAN, null));
    }

    private BowelRecord saveBowel(User user, LocalDateTime bowelAt) {
        return bowelRecordRepository.save(BowelRecord.create(user, bowelAt, false, BristolType.TYPE_4, 0, null));
    }

    @Test
    @DisplayName("기간 조회 - 시작은 포함, 끝은 제외하고 시간순으로 정렬한다")
    void findByPeriod() {
        LocalDateTime start = LocalDateTime.of(2026, 10, 6, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 10, 7, 0, 0);

        BowelRecord evening = saveBowel(me, LocalDateTime.of(2026, 10, 6, 19, 0));
        BowelRecord atStart = saveBowel(me, start);                        // 포함
        BowelRecord lastMinute = saveBowel(me, end.minusNanos(1_000));     // 포함
        saveBowel(me, end);                                                // 제외 (다음날 0시)
        saveBowel(me, start.minusSeconds(1));                              // 제외 (전날)
        saveBowel(other, LocalDateTime.of(2026, 10, 6, 12, 0));            // 제외 (남의 기록)

        List<BowelRecord> result = bowelRecordRepository
                .findAllByUserIdAndBowelAtGreaterThanEqualAndBowelAtLessThanOrderByBowelAtAsc(me.getId(), start, end);

        assertThat(result).extracting(BowelRecord::getId)
                .containsExactly(atStart.getId(), evening.getId(), lastMinute.getId());
    }

    @Test
    @DisplayName("배변 상태는 enum 이름 그대로 저장되고 다시 읽힌다")
    void enumStoredAsString() {
        BowelRecord saved = bowelRecordRepository.saveAndFlush(
                BowelRecord.create(me, LocalDateTime.of(2026, 10, 6, 8, 0), true, BristolType.TYPE_7, 9, "설사"));

        BowelRecord found = bowelRecordRepository.findById(saved.getId()).orElseThrow();

        assertThat(found.getBowelCondition()).isEqualTo(BristolType.TYPE_7);
        assertThat(found.isBleeding()).isTrue();
        assertThat(found.getPainLevel()).isEqualTo(9);
        assertThat(found.getMemo()).isEqualTo("설사");
    }

    @Test
    @DisplayName("isOwnedBy - 작성자 id일 때만 true")
    void isOwnedBy() {
        BowelRecord bowel = saveBowel(me, LocalDateTime.of(2026, 10, 6, 8, 0));

        assertThat(bowel.isOwnedBy(me.getId())).isTrue();
        assertThat(bowel.isOwnedBy(other.getId())).isFalse();
    }
}
