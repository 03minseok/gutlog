package com.gutlog.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/**
 * "오늘"을 구할 때 쓰는 시계. 서비스는 시간대가 한국 기준이라 서울 시간대로 고정한다.
 * 테스트에서는 고정된 시계로 바꿔 날짜에 따라 결과가 달라지지 않게 한다.
 */
@Configuration
public class ClockConfig {
	@Bean
	public Clock clock() {
		return Clock.system(ZoneId.of("Asia/Seoul"));
	}
}
