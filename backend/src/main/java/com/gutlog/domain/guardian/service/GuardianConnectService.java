package com.gutlog.domain.guardian.service;

import com.gutlog.domain.guardian.dto.response.GuardianPatientResponse;
import com.gutlog.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 보호자의 코드 입력으로 연결한다. 이 클래스에는 트랜잭션을 걸지 않는다.
 * GuardianConnectProcessor의 트랜잭션이 커밋된 뒤에 결과를 보고 예외를 던진다. (design.md 5.1)
 */
@Service
@RequiredArgsConstructor
public class GuardianConnectService {
	private final GuardianConnectProcessor connectProcessor;

	// 코드로 연결: Processor의 결과가 성공이면 연결된 환자를 돌려주고, 실패면 그 ErrorCode로 예외를 던진다 (design.md 3.2, 5.1)
	public GuardianPatientResponse connect(Long userId, String code) {
		ConnectResult result = connectProcessor.process(userId, code);
		if(result.isSuccess()){
			return result.response();
		}
		throw new BusinessException(result.errorCode());
	}
}
