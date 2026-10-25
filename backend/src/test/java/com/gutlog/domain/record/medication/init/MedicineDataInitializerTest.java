package com.gutlog.domain.record.medication.init;

import com.gutlog.domain.record.medication.entity.DosageForm;
import com.gutlog.domain.record.medication.entity.Medicine;
import com.gutlog.domain.record.medication.entity.MedicineCategory;
import com.gutlog.domain.record.medication.repository.MedicineRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * 약 마스터 초기 데이터. 실제 data/medicines.csv를 읽는다.
 */
@ExtendWith(MockitoExtension.class)
class MedicineDataInitializerTest {

    @Mock MedicineRepository medicineRepository;
    @InjectMocks MedicineDataInitializer initializer;

    @Test
    @DisplayName("비어 있으면 CSV의 모든 줄을 넣는다 (앞의 4개 열만 사용)")
    @SuppressWarnings("unchecked")
    void seedWhenEmpty() throws Exception {
        given(medicineRepository.count()).willReturn(0L);

        initializer.run(null);

        ArgumentCaptor<List<Medicine>> captor = ArgumentCaptor.forClass(List.class);
        verify(medicineRepository).saveAll(captor.capture());
        List<Medicine> saved = captor.getValue();
        assertThat(saved).hasSize(33);
        assertThat(saved).allMatch(Medicine::isActive);
        assertThat(saved.getFirst().getGenericName()).isEqualTo("메살라진");
        assertThat(saved.getFirst().getBrandName()).isEqualTo("펜타사");
        assertThat(saved.getFirst().getDosageForm()).isEqualTo(DosageForm.TABLET);
        assertThat(saved.getFirst().getCategory()).isEqualTo(MedicineCategory.AMINOSALICYLATE);
    }

    @Test
    @DisplayName("CSV 안에 성분명 + 제품명 + 제형이 겹치는 줄이 없다 (DB unique 제약과 같은 기준)")
    @SuppressWarnings("unchecked")
    void noDuplicateRows() throws Exception {
        given(medicineRepository.count()).willReturn(0L);

        initializer.run(null);

        ArgumentCaptor<List<Medicine>> captor = ArgumentCaptor.forClass(List.class);
        verify(medicineRepository).saveAll(captor.capture());
        assertThat(captor.getValue())
                .extracting(m -> m.getGenericName() + "/" + m.getBrandName() + "/" + m.getDosageForm())
                .doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("이미 약이 있으면 다시 넣지 않는다")
    void skipWhenNotEmpty() throws Exception {
        given(medicineRepository.count()).willReturn(5L);

        initializer.run(null);

        verify(medicineRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("형식이 틀린 줄은 몇 번째 줄인지와 함께 실패한다")
    void malformedLine() {
        assertThatThrownBy(() -> MedicineDataInitializer.parseLine("메살라진,펜타사,PILL,AMINOSALICYLATE,Y,", 3))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("3번째 줄");
        assertThatThrownBy(() -> MedicineDataInitializer.parseLine(",펜타사,TABLET,AMINOSALICYLATE,Y,", 4))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> MedicineDataInitializer.parseLine("메살라진,펜타사", 5))
                .isInstanceOf(IllegalStateException.class);
    }
}
