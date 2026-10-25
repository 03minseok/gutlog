package com.gutlog.domain.record.medication.init;

import com.gutlog.domain.record.medication.entity.DosageForm;
import com.gutlog.domain.record.medication.entity.Medicine;
import com.gutlog.domain.record.medication.entity.MedicineCategory;
import com.gutlog.domain.record.medication.repository.MedicineRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 서버가 켜질 때 약 마스터가 비어 있으면 data/medicines.csv로 채운다. 이미 있으면 넣지 않는다.
 * CSV 열: generic_name, brand_name, dosage_form, category, verified, note — 앞의 4개 열만 읽는다.
 * 형식이 틀린 줄이 있으면 서버 시작을 멈춘다. (잘못된 초기 데이터가 조용히 빠지지 않게)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MedicineDataInitializer implements ApplicationRunner {
	static final String CSV_PATH = "data/medicines.csv";

	private final MedicineRepository medicineRepository;

	@Override
	@Transactional
	public void run(ApplicationArguments args) throws IOException {
		if (medicineRepository.count() > 0) {
			return;
		}
		List<Medicine> medicines = readCsv();
		medicineRepository.saveAll(medicines);
		log.info("약 마스터 초기 데이터 {}건을 넣었습니다.", medicines.size());
	}

	private List<Medicine> readCsv() throws IOException {
		List<Medicine> medicines = new ArrayList<>();
		try (BufferedReader reader = new BufferedReader(
				new InputStreamReader(new ClassPathResource(CSV_PATH).getInputStream(), StandardCharsets.UTF_8))) {
			reader.readLine(); // 헤더
			String line;
			int lineNumber = 1;
			while ((line = reader.readLine()) != null) {
				lineNumber++;
				if (line.isBlank()) {
					continue;
				}
				medicines.add(parseLine(line, lineNumber));
			}
		}
		return medicines;
	}

	static Medicine parseLine(String line, int lineNumber) {
		String[] columns = line.split(",", -1);
		try {
			return Medicine.create(
					requireText(columns[0]),
					requireText(columns[1]),
					DosageForm.valueOf(columns[2].strip()),
					MedicineCategory.valueOf(columns[3].strip()));
		} catch (RuntimeException e) {
			throw new IllegalStateException(CSV_PATH + " " + lineNumber + "번째 줄 형식이 올바르지 않습니다: " + line, e);
		}
	}

	private static String requireText(String value) {
		if (value.isBlank()) {
			throw new IllegalArgumentException("빈 값");
		}
		return value;
	}
}
