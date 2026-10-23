package com.gutlog.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "U001", "사용자를 찾을 수 없습니다."),
    DUPLICATE_USERNAME(HttpStatus.CONFLICT, "U002", "이미 사용 중인 아이디입니다."),
    LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "U003", "아이디 또는 비밀번호가 올바르지 않습니다."),
    INVALID_DATE_RANGE(HttpStatus.BAD_REQUEST, "D001", "날짜 범위가 올바르지 않습니다."),

    MEAL_RECORD_NOT_FOUND(HttpStatus.NOT_FOUND, "M001", "식사 기록을 찾을 수 없습니다."),
    FOOD_NOT_FOUND(HttpStatus.NOT_FOUND, "F001", "음식을 찾을 수 없습니다."),

    BOWEL_RECORD_NOT_FOUND(HttpStatus.NOT_FOUND, "B001", "배변 기록을 찾을 수 없습니다."),

    FUTURE_DATE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "D002", "미래 날짜에는 기록할 수 없습니다."),

    MEDICINE_NOT_FOUND(HttpStatus.NOT_FOUND, "MD001", "약을 찾을 수 없습니다."),
    DUPLICATE_MEDICINE(HttpStatus.CONFLICT, "MD002", "이미 등록된 약입니다."),

    SCHEDULE_NOT_FOUND(HttpStatus.INTERNAL_SERVER_ERROR, "S001", "복용 일정을 찾을 수 없습니다."),
    NOT_IN_USE_ON_DATE(HttpStatus.BAD_REQUEST,"S002", "그 날짜에는 복용 중인 약이 아닙니다."),

    USER_MEDICINE_NOT_FOUND(HttpStatus.NOT_FOUND, "UM001", "복용 약을 찾을 수 없습니다."),
    DUPLICATE_USER_MEDICINE(HttpStatus.CONFLICT, "UM002", "이미 복용 중인 약입니다."),
    INVALID_DOSE_AMOUNT(HttpStatus.BAD_REQUEST, "UM003", "복용량이 올바르지 않습니다."),

    INVALID_DOSE_NUMBER(HttpStatus.BAD_REQUEST, "ML001", "복용 회차가 올바르지 않습니다."),
    DOSE_ORDER_CHECK(HttpStatus.BAD_REQUEST, "ML002", "이전 회차를 먼저 체크해 주세요."),
    DOSE_ORDER_UNCHECK(HttpStatus.BAD_REQUEST, "ML003", "다음 회차를 먼저 취소해 주세요."),
    STOPPED_MEDICINE(HttpStatus.BAD_REQUEST, "ML004", "복용을 중단한 약에는 기록할 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
