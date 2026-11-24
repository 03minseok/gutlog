package com.gutlog.domain.guardian.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class GuardianInviteCodeTest {

    private static final LocalDateTime EXPIRES = LocalDateTime.of(2026, 11, 24, 12, 10);

    @Test
    @DisplayName("만료 판단 - 만료 시각 전이면 유효하고, 같거나 지나면 만료다")
    void isExpired() {
        GuardianInviteCode code = GuardianInviteCode.create(null, "AB12CD", EXPIRES);

        assertThat(code.isExpired(EXPIRES.minusSeconds(1))).isFalse();
        assertThat(code.isExpired(EXPIRES)).isTrue();
        assertThat(code.isExpired(EXPIRES.plusSeconds(1))).isTrue();
    }
}
