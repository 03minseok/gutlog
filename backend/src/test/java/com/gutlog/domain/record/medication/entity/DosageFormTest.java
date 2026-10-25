package com.gutlog.domain.record.medication.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/** 제형별 복용량 단위·간격 규칙 */
class DosageFormTest {

    @ParameterizedTest
    @ValueSource(strings = {"0.5", "1", "1.5", "2.0", "20"})
    @DisplayName("정제: 0.5 단위로 0.5~20정 허용")
    void tabletValid(String amount) {
        assertThat(DosageForm.TABLET.isValidDoseAmount(new BigDecimal(amount))).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "0.3", "1.2", "20.5", "-1"})
    @DisplayName("정제: 0.5 단위가 아니거나 범위 밖이면 거절")
    void tabletInvalid(String amount) {
        assertThat(DosageForm.TABLET.isValidDoseAmount(new BigDecimal(amount))).isFalse();
    }

    @Test
    @DisplayName("내복액: 0.1mL 단위로 0.1~100mL 허용")
    void oralLiquid() {
        assertThat(DosageForm.ORAL_LIQUID.isValidDoseAmount(new BigDecimal("2.3"))).isTrue();
        assertThat(DosageForm.ORAL_LIQUID.isValidDoseAmount(new BigDecimal("0.1"))).isTrue();
        assertThat(DosageForm.ORAL_LIQUID.isValidDoseAmount(new BigDecimal("100"))).isTrue();
        assertThat(DosageForm.ORAL_LIQUID.isValidDoseAmount(new BigDecimal("100.1"))).isFalse();
        assertThat(DosageForm.ORAL_LIQUID.isValidDoseAmount(new BigDecimal("0.05"))).isFalse();
    }

    @Test
    @DisplayName("단위는 제형이 정한다")
    void doseUnit() {
        assertThat(DosageForm.TABLET.getDoseUnit()).isEqualTo("정");
        assertThat(DosageForm.CAPSULE.getDoseUnit()).isEqualTo("캡슐");
        assertThat(DosageForm.GRANULE.getDoseUnit()).isEqualTo("포");
        assertThat(DosageForm.SUPPOSITORY.getDoseUnit()).isEqualTo("개");
        assertThat(DosageForm.ENEMA.getDoseUnit()).isEqualTo("회");
        assertThat(DosageForm.SC_INJECTION.getDoseUnit()).isEqualTo("회");
    }
}
