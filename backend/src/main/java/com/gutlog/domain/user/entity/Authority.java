package com.gutlog.domain.user.entity;

/**
 * 권한. 회원 유형(Role: 환자·보호자·기타)과는 별개다.
 * 회원가입은 항상 USER이고, ADMIN은 DB에서 직접 지정한다.
 */
public enum Authority {
	USER,
	ADMIN
}
