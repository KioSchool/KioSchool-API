package com.kioschool.kioschoolapi.global.error

import org.springframework.http.HttpStatus

enum class ErrorCode(
    val status: HttpStatus,
    val defaultMessage: String,
) {
    // Common
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다."),

    // User
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "로그인에 실패하였습니다."),
    NO_PERMISSION(HttpStatus.UNAUTHORIZED, "권한이 없습니다."),
    DUPLICATE_LOGIN_ID(HttpStatus.BAD_REQUEST, "회원가입 실패: 이미 사용 중인 아이디입니다."),
    DUPLICATE_EMAIL(HttpStatus.BAD_REQUEST, "회원가입 실패: 이미 가입된 이메일입니다."),
    EMAIL_NOT_VERIFIED(HttpStatus.BAD_REQUEST, "회원가입 실패: 이메일 인증이 필요합니다."),

    // Workspace
    WORKSPACE_INACCESSIBLE(HttpStatus.METHOD_NOT_ALLOWED, "접근 불가능한 워크스페이스입니다."),
    WORKSPACE_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 워크스페이스입니다."),
    WORKSPACE_TABLE_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 테이블입니다."),
    NO_PERMISSION_TO_CREATE_WORKSPACE(HttpStatus.UNAUTHORIZED, "워크스페이스를 생성할 권한이 없습니다. 계좌정보를 등록해주세요."),
    NO_PERMISSION_TO_INVITE(HttpStatus.FORBIDDEN, "초대 권한이 없습니다."),
    NO_PERMISSION_TO_JOIN_WORKSPACE(HttpStatus.FORBIDDEN, "워크스페이스를 가입 할 권한이 없습니다."),
    SUPER_ADMIN_WORKSPACE_READ_ONLY(HttpStatus.BAD_REQUEST, "최고 관리자(Super Admin)는 타인의 워크스페이스를 조회만 할 수 있으며, 수정(CUD)은 불가능합니다."),
    TABLE_POSITION_CONFLICT(HttpStatus.CONFLICT, "해당 위치에 이미 다른 테이블이 배치되어 있습니다."),
    INVALID_TABLE_POSITION(HttpStatus.BAD_REQUEST, "테이블 위치가 올바르지 않습니다."),

    // Product
    NOT_FOUND_PRODUCT(HttpStatus.NOT_FOUND, "존재하지 않는 상품입니다."),
    NOT_SELLABLE_PRODUCT(HttpStatus.NOT_ACCEPTABLE, "판매할 수 없는 상품입니다."),
    CANNOT_DELETE_USING_PRODUCT_CATEGORY(HttpStatus.METHOD_NOT_ALLOWED, "해당 카테고리를 사용하는 상품이 존재합니다."),

    // Order
    EMPTY_ORDER_SESSION(HttpStatus.BAD_REQUEST, "예기치 않게 주문 내역이 없는 세션입니다. 세션 기록을 삭제하시겠습니까, 아니면 이대로 저장하시겠습니까?"),
    NO_ORDER_SESSION(HttpStatus.BAD_REQUEST, "테이블이 사용상태가 아니라 주문을 받을 수 없습니다. 주점 관리자에게 문의하세요."),
    ORDER_SESSION_ALREADY_EXIST(HttpStatus.BAD_REQUEST, "이미 테이블이 사용상태입니다."),
    TABLE_HASH_IS_NULL(HttpStatus.BAD_REQUEST, "주문 URL이 유효하지 않습니다. 주점 관리자에게 문의하세요."),

    // Account / Toss
    BANK_NOT_FOUND(HttpStatus.NOT_FOUND, "은행을 찾을 수 없습니다."),
    BANK_TOSS_NAME_NOT_FOUND(HttpStatus.BAD_REQUEST, "해당 은행은 토스 명칭이 등록되어 있지 않습니다."),
    ACCOUNT_HOLDER_NOT_FOUND(HttpStatus.NOT_FOUND, "계좌 정보를 조회할 수 없습니다. 은행과 계좌번호를 다시 확인해주세요. 은행 점검 시간일 수도 있습니다."),
    DIFFERENT_ACCOUNT_NUMBER(HttpStatus.BAD_REQUEST, "토스 계좌와 사용자 계좌가 일치하지 않습니다."),

    // Email
    DUPLICATE_EMAIL_DOMAIN(HttpStatus.BAD_REQUEST, "이미 등록된 도메인입니다."),
    NOT_VERIFIED_EMAIL_DOMAIN(HttpStatus.UNPROCESSABLE_ENTITY, "허용되지 않은 이메일 도메인입니다."),
    EMAIL_SEND_FAILURE(HttpStatus.INTERNAL_SERVER_ERROR, "이메일 전송에 실패했습니다."),

    // Inquiry
    INQUIRY_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 문의입니다."),
    INQUIRY_ALREADY_ANSWERED(HttpStatus.CONFLICT, "이미 답변이 완료된 문의입니다."),
    INQUIRY_ALREADY_CLOSED(HttpStatus.CONFLICT, "이미 종결된 문의입니다."),
    INQUIRY_IMAGE_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "이미지는 최대 5장까지 첨부할 수 있습니다."),
    INQUIRY_IMAGE_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE, "이미지 한 장의 크기는 5MB를 넘을 수 없습니다."),
    INQUIRY_IMAGE_TYPE_NOT_ALLOWED(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "JPEG, PNG, WebP 이미지만 첨부할 수 있습니다."),
    INQUIRY_RATE_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "문의 접수 횟수 제한을 초과했습니다. 잠시 후 다시 시도해 주세요."),

    // Security
    AUTHENTICATION_REQUIRED(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    INVALID_JWT(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
}
