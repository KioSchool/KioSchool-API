package com.kioschool.kioschoolapi.common.exception

class InvalidJwtException : Exception() {
    override val message: String
        get() = "유효하지 않은 토큰입니다."
}