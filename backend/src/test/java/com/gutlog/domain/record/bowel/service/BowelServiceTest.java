package com.gutlog.domain.record.bowel.service;

import com.gutlog.domain.record.bowel.dto.request.BowelCreateRequest;
import com.gutlog.domain.record.bowel.dto.response.BowelResponse;
import com.gutlog.domain.record.bowel.entity.BowelRecord;
import com.gutlog.domain.record.bowel.entity.BristolType;
import com.gutlog.domain.record.bowel.repository.BowelRecordRepository;
import com.gutlog.domain.user.entity.IbdType;
import com.gutlog.domain.user.entity.Role;
import com.gutlog.domain.user.entity.User;
import com.gutlog.domain.user.repository.UserRepository;
import com.gutlog.global.exception.BusinessException;
import com.gutlog.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * BowelService 단위 테스트.
 * 레포지토리를 Mockito로 대체해서 소유권 검사, 기간 계산, 수정 반영 같은 서비스 로직만 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class BowelServiceTest {

    @Mock BowelRecordRepository bowelRecordRepository;
    @Mock UserRepository userRepository;

    @InjectMocks BowelService bowelService;

    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;
    private static final LocalDateTime BOWEL_AT = LocalDateTime.of(2026, 10, 6, 8, 30);

    private static User user(Long id) {
        User user = User.create("user" + id, "ENCODED", "닉" + id, Role.PATIENT, IbdType.ULCERATIVE_COLITIS);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private static BowelRecord bowel(Long id, User owner, LocalDateTime bowelAt) {
        BowelRecord bowel = BowelRecord.create(owner, bowelAt, false, BristolType.TYPE_4, 0, null);
        ReflectionTestUtils.setField(bowel, "id", id);
        return bowel;
    }

    private static BowelCreateRequest request(LocalDateTime bowelAt) {
        return new BowelCreateRequest(bowelAt, true, BristolType.TYPE_6, 7, "복통");
    }

    private static ErrorCode errorCodeOf(Throwable e) {
        return ((BusinessException) e).getErrorCode();
    }

    @Nested
    @DisplayName("배변 기록 생성")
    class Create {

        @Test
        @DisplayName("성공 - 요청 값 그대로 내 기록으로 저장하고 반환한다")
        void success() {
            User user = user(USER_ID);
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));

            BowelResponse response = bowelService.createBowelRecord(USER_ID, request(BOWEL_AT));

            ArgumentCaptor<BowelRecord> captor = ArgumentCaptor.forClass(BowelRecord.class);
            verify(bowelRecordRepository).save(captor.capture());
            BowelRecord saved = captor.getValue();
            assertThat(saved.getUser()).isSameAs(user);
            assertThat(saved.getBowelAt()).isEqualTo(BOWEL_AT);
            assertThat(saved.getBleeding()).isTrue();
            assertThat(saved.getBowelCondition()).isEqualTo(BristolType.TYPE_6);
            assertThat(saved.getPainLevel()).isEqualTo(7);
            assertThat(saved.getMemo()).isEqualTo("복통");

            assertThat(response.bowelAt()).isEqualTo(BOWEL_AT);
            assertThat(response.bleeding()).isTrue();
            assertThat(response.bowelCondition()).isEqualTo(BristolType.TYPE_6);
            assertThat(response.painLevel()).isEqualTo(7);
            assertThat(response.memo()).isEqualTo("복통");
        }

        @Test
        @DisplayName("실패 - 유저가 없으면 USER_NOT_FOUND, 저장하지 않는다")
        void userNotFound() {
            given(userRepository.findById(USER_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> bowelService.createBowelRecord(USER_ID, request(BOWEL_AT)))
                    .isInstanceOf(BusinessException.class)
                    .extracting(BowelServiceTest::errorCodeOf)
                    .isEqualTo(ErrorCode.USER_NOT_FOUND);

            verify(bowelRecordRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("단건 조회")
    class GetOne {

        @Test
        @DisplayName("성공 - 내 기록을 반환한다")
        void success() {
            given(bowelRecordRepository.findById(10L)).willReturn(Optional.of(bowel(10L, user(USER_ID), BOWEL_AT)));

            BowelResponse response = bowelService.getBowelRecord(USER_ID, 10L);

            assertThat(response.id()).isEqualTo(10L);
            assertThat(response.bowelAt()).isEqualTo(BOWEL_AT);
        }

        @Test
        @DisplayName("실패 - 없는 기록이면 BOWEL_RECORD_NOT_FOUND")
        void notFound() {
            given(bowelRecordRepository.findById(10L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> bowelService.getBowelRecord(USER_ID, 10L))
                    .extracting(BowelServiceTest::errorCodeOf)
                    .isEqualTo(ErrorCode.BOWEL_RECORD_NOT_FOUND);
        }

        @Test
        @DisplayName("실패 - 남의 기록이면 존재를 숨기고 BOWEL_RECORD_NOT_FOUND")
        void notOwner() {
            given(bowelRecordRepository.findById(10L)).willReturn(Optional.of(bowel(10L, user(OTHER_USER_ID), BOWEL_AT)));

            assertThatThrownBy(() -> bowelService.getBowelRecord(USER_ID, 10L))
                    .extracting(BowelServiceTest::errorCodeOf)
                    .isEqualTo(ErrorCode.BOWEL_RECORD_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("기간 조회")
    class GetByPeriod {

        @Test
        @DisplayName("from~to를 [from 00:00, to+1일 00:00) 범위로 바꿔서 조회한다")
        void rangeConversion() {
            given(bowelRecordRepository.findAllByUserIdAndBowelAtGreaterThanEqualAndBowelAtLessThanOrderByBowelAtAsc(any(), any(), any()))
                    .willReturn(List.of());

            bowelService.getAllBowelRecordsByPeriod(USER_ID, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 6));

            verify(bowelRecordRepository).findAllByUserIdAndBowelAtGreaterThanEqualAndBowelAtLessThanOrderByBowelAtAsc(
                    USER_ID,
                    LocalDateTime.of(2026, 10, 1, 0, 0),
                    LocalDateTime.of(2026, 10, 7, 0, 0));
        }

        @Test
        @DisplayName("레포지토리가 준 순서 그대로 응답으로 변환한다")
        void mapInOrder() {
            User user = user(USER_ID);
            given(bowelRecordRepository.findAllByUserIdAndBowelAtGreaterThanEqualAndBowelAtLessThanOrderByBowelAtAsc(any(), any(), any()))
                    .willReturn(List.of(
                            bowel(10L, user, BOWEL_AT),
                            bowel(11L, user, BOWEL_AT.plusHours(3))));

            List<BowelResponse> result = bowelService.getAllBowelRecordsByPeriod(
                    USER_ID, LocalDate.of(2026, 10, 6), LocalDate.of(2026, 10, 6));

            assertThat(result).extracting(BowelResponse::id).containsExactly(10L, 11L);
        }

        @Test
        @DisplayName("기록이 없으면 빈 리스트")
        void empty() {
            given(bowelRecordRepository.findAllByUserIdAndBowelAtGreaterThanEqualAndBowelAtLessThanOrderByBowelAtAsc(any(), any(), any()))
                    .willReturn(List.of());

            assertThat(bowelService.getAllBowelRecordsByPeriod(
                    USER_ID, LocalDate.of(2026, 10, 6), LocalDate.of(2026, 10, 6))).isEmpty();
        }

        @Test
        @DisplayName("실패 - from이 to보다 늦으면 INVALID_DATE_RANGE, 조회하지 않는다")
        void invalidRange() {
            assertThatThrownBy(() -> bowelService.getAllBowelRecordsByPeriod(
                    USER_ID, LocalDate.of(2026, 10, 7), LocalDate.of(2026, 10, 6)))
                    .extracting(BowelServiceTest::errorCodeOf)
                    .isEqualTo(ErrorCode.INVALID_DATE_RANGE);

            verify(bowelRecordRepository, never())
                    .findAllByUserIdAndBowelAtGreaterThanEqualAndBowelAtLessThanOrderByBowelAtAsc(any(), any(), any());
        }
    }

    @Nested
    @DisplayName("수정")
    class Edit {

        @Test
        @DisplayName("성공 - 엔티티의 모든 필드를 요청 값으로 바꾼다")
        void success() {
            BowelRecord bowel = bowel(10L, user(USER_ID), BOWEL_AT);
            LocalDateTime newTime = BOWEL_AT.plusHours(1);
            given(bowelRecordRepository.findById(10L)).willReturn(Optional.of(bowel));

            BowelResponse response = bowelService.editBowelRecord(USER_ID, 10L, request(newTime));

            assertThat(bowel.getBowelAt()).isEqualTo(newTime);
            assertThat(bowel.getBleeding()).isTrue();
            assertThat(bowel.getBowelCondition()).isEqualTo(BristolType.TYPE_6);
            assertThat(bowel.getPainLevel()).isEqualTo(7);
            assertThat(bowel.getMemo()).isEqualTo("복통");
            assertThat(response.id()).isEqualTo(10L);
            assertThat(response.bowelAt()).isEqualTo(newTime);
        }

        @Test
        @DisplayName("실패 - 남의 기록은 수정하지 않는다")
        void notOwner() {
            BowelRecord bowel = bowel(10L, user(OTHER_USER_ID), BOWEL_AT);
            given(bowelRecordRepository.findById(10L)).willReturn(Optional.of(bowel));

            assertThatThrownBy(() -> bowelService.editBowelRecord(USER_ID, 10L, request(BOWEL_AT.plusHours(1))))
                    .extracting(BowelServiceTest::errorCodeOf)
                    .isEqualTo(ErrorCode.BOWEL_RECORD_NOT_FOUND);

            assertThat(bowel.getBowelAt()).isEqualTo(BOWEL_AT);
            assertThat(bowel.getPainLevel()).isZero();
        }
    }

    @Nested
    @DisplayName("삭제")
    class Delete {

        @Test
        @DisplayName("성공 - 내 기록을 삭제한다")
        void success() {
            BowelRecord bowel = bowel(10L, user(USER_ID), BOWEL_AT);
            given(bowelRecordRepository.findById(10L)).willReturn(Optional.of(bowel));

            bowelService.deleteBowelRecord(USER_ID, 10L);

            verify(bowelRecordRepository).delete(bowel);
        }

        @Test
        @DisplayName("실패 - 남의 기록은 지우지 않는다")
        void notOwner() {
            given(bowelRecordRepository.findById(10L)).willReturn(Optional.of(bowel(10L, user(OTHER_USER_ID), BOWEL_AT)));

            assertThatThrownBy(() -> bowelService.deleteBowelRecord(USER_ID, 10L))
                    .extracting(BowelServiceTest::errorCodeOf)
                    .isEqualTo(ErrorCode.BOWEL_RECORD_NOT_FOUND);

            verify(bowelRecordRepository, never()).delete(any());
        }

        @Test
        @DisplayName("실패 - 없는 기록이면 BOWEL_RECORD_NOT_FOUND")
        void notFound() {
            given(bowelRecordRepository.findById(10L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> bowelService.deleteBowelRecord(USER_ID, 10L))
                    .extracting(BowelServiceTest::errorCodeOf)
                    .isEqualTo(ErrorCode.BOWEL_RECORD_NOT_FOUND);

            verify(bowelRecordRepository, never()).delete(any());
        }
    }
}
