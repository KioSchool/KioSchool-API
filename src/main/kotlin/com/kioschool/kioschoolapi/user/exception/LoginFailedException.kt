package com.kioschool.kioschoolapi.user.exception

class LoginFailedException : Exception() {
    override val message: String
        get() = "로그인에 실패하였습니다."
}