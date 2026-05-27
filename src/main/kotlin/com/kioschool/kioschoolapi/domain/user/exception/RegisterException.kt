package com.kioschool.kioschoolapi.domain.user.exception

import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException

class RegisterException(reason: Reason) :
    ResponseStatusException(HttpStatus.BAD_REQUEST, "회원가입 실패: ${reason.message}") {

    enum class Reason(val message: String) {
        DUPLICATE_LOGIN_ID("이미 사용 중인 아이디입니다."),
        DUPLICATE_EMAIL("이미 가입된 이메일입니다."),
        EMAIL_NOT_VERIFIED("이메일 인증이 필요합니다."),
    }
}
