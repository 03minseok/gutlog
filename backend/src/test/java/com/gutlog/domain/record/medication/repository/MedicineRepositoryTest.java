package com.gutlog.domain.record.medication.repository;

import com.gutlog.domain.record.medication.entity.DosageForm;
import com.gutlog.domain.record.medication.entity.Medicine;
import com.gutlog.domain.record.medication.entity.MedicineCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 약 마스터 쿼리. @DataJpaTest는 ApplicationRunner(초기 데이터)를 띄우지 않아 빈 테이블에서 시작한다.
 */
@DataJpaTest
class MedicineRepositoryTest {

    @Autowired MedicineRepository medicineRepository;

    private Medicine pentasaTablet;
    private Medicine pentasaSuppository;
    private Medicine humira;
    private Medicine inactive;

    @BeforeEach
    void setUp() {
        pentasaTablet = save("메살라진", "펜타사", DosageForm.TABLET, MedicineCategory.AMINOSALICYLATE);
        pentasaSuppository = save("메살라진", "펜타사", DosageForm.SUPPOSITORY, MedicineCategory.AMINOSALICYLATE);
        humira = save("아달리무맙", "휴미라", DosageForm.SC_INJECTION, MedicineCategory.BIOLOGIC);
        inactive = Medicine.create("메살라진", "단종약", DosageForm.TABLET, MedicineCategory.AMINOSALICYLATE);
        inactive.deactivate();
        medicineRepository.save(inactive);
    }

    private Medicine save(String generic, String brand, DosageForm form, MedicineCategory category) {
        return medicineRepository.save(Medicine.create(generic, brand, form, category));
    }

    @Test
    @DisplayName("검색 - 성분명이나 제품명 일부로 찾고, 비활성 약은 빠진다")
    void searchActive() {
        assertThat(medicineRepository.searchActive("메살라"))
                .extracting(Medicine::getId)
                .containsExactlyInAnyOrder(pentasaTablet.getId(), pentasaSuppository.getId());
        assertThat(medicineRepository.searchActive("휴미"))
                .extracting(Medicine::getId)
                .containsExactly(humira.getId());
        assertThat(medicineRepository.searchActive("단종")).isEmpty();
    }

    @Test
    @DisplayName("활성 약 전체 - 성분명, 제품명, 제형 순으로 정렬 (제형은 저장된 문자열 기준이라 SUPPOSITORY < TABLET)")
    void findAllActive() {
        assertThat(medicineRepository.findAllActive())
                .extracting(Medicine::getId)
                .containsExactly(pentasaSuppository.getId(), pentasaTablet.getId(), humira.getId());
    }

    @Test
    @DisplayName("관리자 목록은 비활성 약도 포함한다")
    void findAllOrdered() {
        assertThat(medicineRepository.findAllOrdered()).extracting(Medicine::getId).contains(inactive.getId()).hasSize(4);
    }

    @Test
    @DisplayName("비활성 약은 findByIdAndActiveTrue로 찾을 수 없다")
    void findActiveById() {
        assertThat(medicineRepository.findByIdAndActiveTrue(humira.getId())).isPresent();
        assertThat(medicineRepository.findByIdAndActiveTrue(inactive.getId())).isEmpty();
    }

    @Test
    @DisplayName("같은 성분명 + 제품명이라도 제형이 다르면 저장된다. 셋 다 같으면 unique 위반")
    void uniqueByNameAndForm() {
        assertThat(pentasaSuppository.getId()).isNotEqualTo(pentasaTablet.getId());

        assertThatThrownBy(() -> medicineRepository.saveAndFlush(
                Medicine.create("메살라진", "펜타사", DosageForm.TABLET, MedicineCategory.AMINOSALICYLATE)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
