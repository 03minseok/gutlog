package com.gutlog.domain.record.weight.service;

import com.gutlog.domain.record.weight.dto.request.WeightRequest;
import com.gutlog.domain.record.weight.dto.response.WeightResponse;
import com.gutlog.domain.record.weight.entity.WeightRecord;
import com.gutlog.domain.record.weight.repository.WeightRecordRepository;
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
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
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
 * WeightService 단위 테스트.
 * 레포지토리는 Mockito로, 시계는 고정 시계(한국 시간 2026-10-06 정오)로 대신한다.
 */
@ExtendWith(MockitoExtension.class)
class WeightServiceTest {

    @Mock WeightRecordRepository weightRecordRepository;
    @Mock UserRepository userRepository;

    WeightService weightService;

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 6);
    private static final Long USER_ID = 1L;

    @BeforeEach
    void setUp() {
        Clock fixed = Clock.fixed(TODAY.atTime(12, 0).atZone(SEOUL).toInstant(), SEOUL);
        weightService = new WeightService(weightRecordRepository, userRepository, fixed);
    }

    private static User user(Long id) {
        User user = User.create("user" + id, "ENCODED", "닉" + id, Role.PATIENT, IbdType.ULCERATIVE_COLITIS);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private static WeightRecord weight(User owner, LocalDate date, String value) {
        return WeightRecord.create(owner, date, new BigDecimal(value));
    }

    private static WeightRequest request(String value) {
        return new WeightRequest(new BigDecimal(value));
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
            given(weightRecordRepository.findByUserIdAndRecordDate(USER_ID, TODAY)).willReturn(Optional.empty());
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
            given(weightRecordRepository.save(any(WeightRecord.class))).willAnswer(inv -> inv.getArgument(0));

            WeightResponse response = weightService.saveWeightRecord(USER_ID, TODAY, request("62.35"));

            ArgumentCaptor<WeightRecord> captor = ArgumentCaptor.forClass(WeightRecord.class);
            verify(weightRecordRepository).save(captor.capture());
            assertThat(captor.getValue().getUser()).isSameAs(user);
            assertThat(captor.getValue().getRecordDate()).isEqualTo(TODAY);
            assertThat(response.date()).isEqualTo(TODAY);
            assertThat(response.weight()).isEqualByComparingTo("62.35");
        }

        @Test
        @DisplayName("그 날짜 기록이 있으면 새로 만들지 않고 값을 바꾼다")
        void updateWhenPresent() {
            LocalDate past = TODAY.minusDays(3);
            WeightRecord existing = weight(user(USER_ID), past, "60.00");
            given(weightRecordRepository.findByUserIdAndRecordDate(USER_ID, past)).willReturn(Optional.of(existing));

            WeightResponse response = weightService.saveWeightRecord(USER_ID, past, request("59.50"));

            assertThat(existing.getWeight()).isEqualByComparingTo("59.50");
            assertThat(response.date()).isEqualTo(past);
            assertThat(response.weight()).isEqualByComparingTo("59.50");
            verify(weightRecordRepository, never()).save(any());
            verifyNoInteractions(userRepository);
        }

        @Test
        @DisplayName("오늘(한국 시간)은 등록할 수 있다")
        void todayAllowed() {
            given(weightRecordRepository.findByUserIdAndRecordDate(USER_ID, TODAY)).willReturn(Optional.empty());
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(user(USER_ID)));
            given(weightRecordRepository.save(any(WeightRecord.class))).willAnswer(inv -> inv.getArgument(0));

            assertThat(weightService.saveWeightRecord(USER_ID, TODAY, request("60")).date()).isEqualTo(TODAY);
        }

        @Test
        @DisplayName("미래 날짜는 FUTURE_DATE_NOT_ALLOWED, 아무것도 조회·저장하지 않는다")
        void futureDateRejected() {
            assertThatThrownBy(() -> weightService.saveWeightRecord(USER_ID, TODAY.plusDays(1), request("60")))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(errorCodeOf(e)).isEqualTo(ErrorCode.FUTURE_DATE_NOT_ALLOWED));
            verifyNoInteractions(weightRecordRepository, userRepository);
        }

        @Test
        @DisplayName("오늘 판단은 서버 시계의 한국 시간 기준이다 (UTC로는 전날이어도 한국 날짜를 쓴다)")
        void todayIsSeoulDate() {
            // 한국 2026-10-07 00:30 = UTC 2026-10-06 15:30
            Clock justAfterMidnight = Clock.fixed(Instant.parse("2026-10-06T15:30:00Z"), SEOUL);
            WeightService service = new WeightService(weightRecordRepository, userRepository, justAfterMidnight);
            LocalDate seoulToday = LocalDate.of(2026, 10, 7);
            given(weightRecordRepository.findByUserIdAndRecordDate(USER_ID, seoulToday)).willReturn(Optional.empty());
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(user(USER_ID)));
            given(weightRecordRepository.save(any(WeightRecord.class))).willAnswer(inv -> inv.getArgument(0));

            assertThat(service.saveWeightRecord(USER_ID, seoulToday, request("60")).date()).isEqualTo(seoulToday);
        }

        @Test
        @DisplayName("새로 만들 때 유저가 없으면 USER_NOT_FOUND")
        void userNotFound() {
            given(weightRecordRepository.findByUserIdAndRecordDate(USER_ID, TODAY)).willReturn(Optional.empty());
            given(userRepository.findById(USER_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> weightService.saveWeightRecord(USER_ID, TODAY, request("60")))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(errorCodeOf(e)).isEqualTo(ErrorCode.USER_NOT_FOUND));
            verify(weightRecordRepository, never()).save(any());
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
            given(weightRecordRepository.findAllByUserIdAndRecordDateBetweenOrderByRecordDateAsc(USER_ID, from, TODAY))
                    .willReturn(List.of(weight(user, from, "60.00"), weight(user, TODAY, "60.50")));

            List<WeightResponse> result = weightService.getWeightRecordsByPeriod(USER_ID, from, TODAY);

            assertThat(result).extracting(WeightResponse::date).containsExactly(from, TODAY);
            assertThat(result.get(1).weight()).isEqualByComparingTo("60.50");
        }

        @Test
        @DisplayName("from과 to가 같으면 하루만 조회한다")
        void sameDay() {
            given(weightRecordRepository.findAllByUserIdAndRecordDateBetweenOrderByRecordDateAsc(USER_ID, TODAY, TODAY))
                    .willReturn(List.of());

            assertThat(weightService.getWeightRecordsByPeriod(USER_ID, TODAY, TODAY)).isEmpty();
        }

        @Test
        @DisplayName("from이 to보다 늦으면 INVALID_DATE_RANGE")
        void invalidRange() {
            assertThatThrownBy(() -> weightService.getWeightRecordsByPeriod(USER_ID, TODAY, TODAY.minusDays(1)))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(errorCodeOf(e)).isEqualTo(ErrorCode.INVALID_DATE_RANGE));
            verifyNoInteractions(weightRecordRepository);
        }
    }

    @Nested
    @DisplayName("삭제")
    class Delete {

        @Test
        @DisplayName("그 날짜의 내 기록을 지운다")
        void success() {
            WeightRecord existing = weight(user(USER_ID), TODAY, "60.00");
            given(weightRecordRepository.findByUserIdAndRecordDate(USER_ID, TODAY)).willReturn(Optional.of(existing));

            weightService.deleteWeightRecord(USER_ID, TODAY);

            verify(weightRecordRepository).delete(existing);
        }

        @Test
        @DisplayName("그 날짜에 내 기록이 없으면 WEIGHT_RECORD_NOT_FOUND")
        void notFound() {
            given(weightRecordRepository.findByUserIdAndRecordDate(USER_ID, TODAY)).willReturn(Optional.empty());

            assertThatThrownBy(() -> weightService.deleteWeightRecord(USER_ID, TODAY))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(errorCodeOf(e)).isEqualTo(ErrorCode.WEIGHT_RECORD_NOT_FOUND));
            verify(weightRecordRepository, never()).delete(any());
        }
    }
}
