package com.kioschool.kioschoolapi.workspace.exception

class NoPermissionToInviteException : Exception() {
    override val message: String
        get() = "초대 권한이 없습니다."
}