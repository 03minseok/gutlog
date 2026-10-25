package com.gutlog.domain.record.medication.service;

import com.gutlog.domain.record.medication.dto.request.MedicineRequest;
import com.gutlog.domain.record.medication.dto.response.MedicineResponse;
import com.gutlog.domain.record.medication.entity.DosageForm;
import com.gutlog.domain.record.medication.entity.Medicine;
import com.gutlog.domain.record.medication.entity.MedicineCategory;
import com.gutlog.domain.record.medication.repository.MedicineRepository;
import com.gutlog.global.exception.BusinessException;
import com.gutlog.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AdminMedicineServiceTest {

    @Mock MedicineRepository medicineRepository;
    @InjectMocks AdminMedicineService adminMedicineService;

    private static final MedicineRequest REQUEST =
            new MedicineRequest(" 메살라진 ", " 펜타사 ", DosageForm.TABLET, MedicineCategory.AMINOSALICYLATE);

    private static Medicine medicine(Long id) {
        Medicine medicine = Medicine.create("메살라진", "펜타사", DosageForm.TABLET, MedicineCategory.AMINOSALICYLATE);
        ReflectionTestUtils.setField(medicine, "id", id);
        return medicine;
    }

    private static ErrorCode errorCodeOf(Throwable e) {
        return ((BusinessException) e).getErrorCode();
    }

    @Test
    @DisplayName("추가 - 이름 앞뒤 공백을 지우고 활성 상태로 저장한다")
    void create() {
        given(medicineRepository.findByGenericNameAndBrandNameAndDosageForm("메살라진", "펜타사", DosageForm.TABLET))
                .willReturn(Optional.empty());

        MedicineResponse response = adminMedicineService.createMedicine(REQUEST);

        verify(medicineRepository).save(any(Medicine.class));
        assertThat(response.genericName()).isEqualTo("메살라진");
        assertThat(response.brandName()).isEqualTo("펜타사");
        assertThat(response.doseUnit()).isEqualTo("정");
        assertThat(response.active()).isTrue();
    }

    @Test
    @DisplayName("추가 실패 - 같은 성분명 + 제품명 + 제형이 있으면 DUPLICATE_MEDICINE")
    void createDuplicate() {
        given(medicineRepository.findByGenericNameAndBrandNameAndDosageForm("메살라진", "펜타사", DosageForm.TABLET))
                .willReturn(Optional.of(medicine(1L)));

        assertThatThrownBy(() -> adminMedicineService.createMedicine(REQUEST))
                .extracting(AdminMedicineServiceTest::errorCodeOf)
                .isEqualTo(ErrorCode.DUPLICATE_MEDICINE);
        verify(medicineRepository, never()).save(any());
    }

    @Test
    @DisplayName("수정 - 이름을 그대로 두고 분류만 바꿔도 자기 자신과는 중복이 아니다")
    void editSelf() {
        Medicine target = medicine(1L);
        given(medicineRepository.findById(1L)).willReturn(Optional.of(target));
        given(medicineRepository.findByGenericNameAndBrandNameAndDosageForm("메살라진", "펜타사", DosageForm.TABLET))
                .willReturn(Optional.of(target));

        adminMedicineService.editMedicine(1L,
                new MedicineRequest("메살라진", "펜타사", DosageForm.TABLET, MedicineCategory.OTHER));

        assertThat(target.getCategory()).isEqualTo(MedicineCategory.OTHER);
    }

    @Test
    @DisplayName("수정 실패 - 다른 약과 이름·제형이 겹치면 DUPLICATE_MEDICINE, 값은 그대로")
    void editDuplicate() {
        Medicine target = medicine(1L);
        ReflectionTestUtils.setField(target, "brandName", "아사콜");
        given(medicineRepository.findById(1L)).willReturn(Optional.of(target));
        given(medicineRepository.findByGenericNameAndBrandNameAndDosageForm("메살라진", "펜타사", DosageForm.TABLET))
                .willReturn(Optional.of(medicine(2L)));

        assertThatThrownBy(() -> adminMedicineService.editMedicine(1L, REQUEST))
                .extracting(AdminMedicineServiceTest::errorCodeOf)
                .isEqualTo(ErrorCode.DUPLICATE_MEDICINE);
        assertThat(target.getBrandName()).isEqualTo("아사콜");
    }

    @Test
    @DisplayName("비활성화 / 재활성화")
    void toggleActive() {
        Medicine target = medicine(1L);
        given(medicineRepository.findById(1L)).willReturn(Optional.of(target));

        assertThat(adminMedicineService.deactivateMedicine(1L).active()).isFalse();
        assertThat(target.isActive()).isFalse();
        assertThat(adminMedicineService.activateMedicine(1L).active()).isTrue();
        assertThat(target.isActive()).isTrue();
    }

    @Test
    @DisplayName("없는 약이면 MEDICINE_NOT_FOUND")
    void notFound() {
        given(medicineRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> adminMedicineService.deactivateMedicine(99L))
                .extracting(AdminMedicineServiceTest::errorCodeOf)
                .isEqualTo(ErrorCode.MEDICINE_NOT_FOUND);
    }
}
