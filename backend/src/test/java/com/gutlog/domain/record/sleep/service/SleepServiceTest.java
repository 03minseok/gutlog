package com.gutlog.domain.record.sleep.service;

import com.gutlog.domain.record.sleep.dto.request.SleepRequest;
import com.gutlog.domain.record.sleep.dto.response.SleepResponse;
import com.gutlog.domain.record.sleep.entity.SleepRecord;
import com.gutlog.domain.record.sleep.repository.SleepRecordRepository;
import com.gutlog.domain.user.entity.IbdType;
import com.gutlog.domain.user.entity.Role;
import com.gutlog.domain.user.entity.User;
import com.gutlog.domain.user.repository.UserRepository;
import com.gutlog.global.exception.BusinessException;
import com.gutlog.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * SleepService 단위 테스트.
 * 레포지토리는 Mockito로, 시계는 고정 시계(한국 시간 2026-10-06 정오)로 대신한다.
 */
@ExtendWith(MockitoExtension.class)
class SleepServiceTest {

    @Mock SleepRecordRepository sleepRecordRepository;
    @Mock UserRepository userRepository;

    SleepService sleepService;

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 6);
    private static final Long USER_ID = 1L;

    @BeforeEach
    void setUp() {
        Clock fixed = Clock.fixed(TODAY.atTime(12, 0).atZone(SEOUL).toInstant(), SEOUL);
        sleepService = new SleepService(sleepRecordRepository, userRepository, fixed);
    }

    private static User user(Long id) {
        User user = User.create("user" + id, "ENCODED", "닉" + id, Role.PATIENT, IbdType.ULCERATIVE_COLITIS);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private static SleepRecord sleep(User owner, LocalDate date, int minutes) {
        return SleepRecord.create(owner, date, minutes);
    }

    private static SleepRequest request(int minutes) {
        return new SleepRequest(minutes);
    }

    private static ErrorCode errorCodeOf(Throwable e) {
        return ((BusinessException) e).getErrorCode();
    }

    @Nested
    @DisplayName("등록·수정")
    class Save {

        @Test
        @DisplayName("그 날짜 기록이 없으면 새로 만든다")
        void createWhenAbsent() {
            User user = user(USER_ID);
            given(sleepRecordRepository.findByUserIdAndRecordDate(USER_ID, TODAY)).willReturn(Optional.empty());
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
            given(sleepRecordRepository.save(any(SleepRecord.class))).willAnswer(inv -> inv.getArgument(0));

            SleepResponse response = sleepService.saveSleepRecord(USER_ID, TODAY, request(450));

            ArgumentCaptor<SleepRecord> captor = ArgumentCaptor.forClass(SleepRecord.class);
            verify(sleepRecordRepository).save(captor.capture());
            assertThat(captor.getValue().getUser()).isSameAs(user);
            assertThat(captor.getValue().getRecordDate()).isEqualTo(TODAY);
            assertThat(captor.getValue().getSleepMinutes()).isEqualTo(450);
            assertThat(response.date()).isEqualTo(TODAY);
            assertThat(response.sleepMinutes()).isEqualTo(450);
        }

        @Test
        @DisplayName("그 날짜 기록이 있으면 새로 만들지 않고 값을 바꾼다")
        void updateWhenPresent() {
            LocalDate past = TODAY.minusDays(3);
            SleepRecord existing = sleep(user(USER_ID), past, 420);
            given(sleepRecordRepository.findByUserIdAndRecordDate(USER_ID, past)).willReturn(Optional.of(existing));

            SleepResponse response = sleepService.saveSleepRecord(USER_ID, past, request(360));

            assertThat(existing.getSleepMinutes()).isEqualTo(360);
            assertThat(response.date()).isEqualTo(past);
            assertThat(response.sleepMinutes()).isEqualTo(360);
            verify(sleepRecordRepository, never()).save(any());
            verifyNoInteractions(userRepository);
        }

        @Test
        @DisplayName("오늘(한국 시간)은 등록할 수 있다")
        void todayAllowed() {
            given(sleepRecordRepository.findByUserIdAndRecordDate(USER_ID, TODAY)).willReturn(Optional.empty());
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(user(USER_ID)));
            given(sleepRecordRepository.save(any(SleepRecord.class))).willAnswer(inv -> inv.getArgument(0));

            assertThat(sleepService.saveSleepRecord(USER_ID, TODAY, request(480)).date()).isEqualTo(TODAY);
        }

        @Test
        @DisplayName("미래 날짜는 FUTURE_DATE_NOT_ALLOWED, 아무것도 조회·저장하지 않는다")
        void futureDateRejected() {
            assertThatThrownBy(() -> sleepService.saveSleepRecord(USER_ID, TODAY.plusDays(1), request(480)))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(errorCodeOf(e)).isEqualTo(ErrorCode.FUTURE_DATE_NOT_ALLOWED));
            verifyNoInteractions(sleepRecordRepository, userRepository);
        }

        @Test
        @DisplayName("오늘 판단은 서버 시계의 한국 시간 기준이다 (UTC로는 전날이어도 한국 날짜를 쓴다)")
        void todayIsSeoulDate() {
            // 한국 2026-10-07 00:30 = UTC 2026-10-06 15:30
            Clock justAfterMidnight = Clock.fixed(Instant.parse("2026-10-06T15:30:00Z"), SEOUL);
            SleepService service = new SleepService(sleepRecordRepository, userRepository, justAfterMidnight);
            LocalDate seoulToday = LocalDate.of(2026, 10, 7);
            given(sleepRecordRepository.findByUserIdAndRecordDate(USER_ID, seoulToday)).willReturn(Optional.empty());
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(user(USER_ID)));
            given(sleepRecordRepository.save(any(SleepRecord.class))).willAnswer(inv -> inv.getArgument(0));

            assertThat(service.saveSleepRecord(USER_ID, seoulToday, request(480)).date()).isEqualTo(seoulToday);
        }

        @Test
        @DisplayName("새로 만들 때 유저가 없으면 USER_NOT_FOUND")
        void userNotFound() {
            given(sleepRecordRepository.findByUserIdAndRecordDate(USER_ID, TODAY)).willReturn(Optional.empty());
            given(userRepository.findById(USER_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> sleepService.saveSleepRecord(USER_ID, TODAY, request(480)))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(errorCodeOf(e)).isEqualTo(ErrorCode.USER_NOT_FOUND));
            verify(sleepRecordRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("기간 조회")
    class Period {

        @Test
        @DisplayName("from, to를 그대로 넘기고 결과를 응답으로 바꾼다")
        void success() {
            LocalDate from = TODAY.minusDays(6);
            User user = user(USER_ID);
            given(sleepRecordRepository.findAllByUserIdAndRecordDateBetweenOrderByRecordDateAsc(USER_ID, from, TODAY))
                    .willReturn(List.of(sleep(user, from, 420), sleep(user, TODAY, 480)));

            List<SleepResponse> result = sleepService.getSleepRecordsByPeriod(USER_ID, from, TODAY);

            assertThat(result).extracting(SleepResponse::date).containsExactly(from, TODAY);
            assertThat(result).extracting(SleepResponse::sleepMinutes).containsExactly(420, 480);
        }

        @Test
        @DisplayName("from과 to가 같으면 하루만 조회한다")
        void sameDay() {
            given(sleepRecordRepository.findAllByUserIdAndRecordDateBetweenOrderByRecordDateAsc(USER_ID, TODAY, TODAY))
                    .willReturn(List.of());

            assertThat(sleepService.getSleepRecordsByPeriod(USER_ID, TODAY, TODAY)).isEmpty();
        }

        @Test
        @DisplayName("from이 to보다 늦으면 INVALID_DATE_RANGE")
        void invalidRange() {
            assertThatThrownBy(() -> sleepService.getSleepRecordsByPeriod(USER_ID, TODAY, TODAY.minusDays(1)))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(errorCodeOf(e)).isEqualTo(ErrorCode.INVALID_DATE_RANGE));
            verifyNoInteractions(sleepRecordRepository);
        }
    }

    @Nested
    @DisplayName("삭제")
    class Delete {

        @Test
        @DisplayName("그 날짜의 내 기록을 지운다")
        void success() {
            SleepRecord existing = sleep(user(USER_ID), TODAY, 480);
            given(sleepRecordRepository.findByUserIdAndRecordDate(USER_ID, TODAY)).willReturn(Optional.of(existing));

            sleepService.deleteSleepRecord(USER_ID, TODAY);

            verify(sleepRecordRepository).delete(existing);
        }

        @Test
        @DisplayName("그 날짜에 내 기록이 없으면 SLEEP_RECORD_NOT_FOUND")
        void notFound() {
            given(sleepRecordRepository.findByUserIdAndRecordDate(USER_ID, TODAY)).willReturn(Optional.empty());

            assertThatThrownBy(() -> sleepService.deleteSleepRecord(USER_ID, TODAY))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(errorCodeOf(e)).isEqualTo(ErrorCode.SLEEP_RECORD_NOT_FOUND));
            verify(sleepRecordRepository, never()).delete(any());
        }
    }
}
