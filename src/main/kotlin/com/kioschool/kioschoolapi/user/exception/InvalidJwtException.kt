package com.kioschool.kioschoolapi.user.exception

class InvalidJwtException : Exception() {
    override val message: String
        get() = "유효하지 않은 토큰입니다."
}