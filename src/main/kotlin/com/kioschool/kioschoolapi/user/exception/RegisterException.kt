package com.kioschool.kioschoolapi.user.exception

class RegisterException : Exception() {
    override val message: String
        get() = "회원가입에 실패했습니다."
}