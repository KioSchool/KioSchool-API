package com.kioschool.kioschoolapi.user.exception

class NoPermissionException : Exception() {
    override val message: String
        get() = "권한이 없습니다."
}