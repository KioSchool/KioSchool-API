package com.kioschool.kioschoolapi.workspace.exception

class NoPermissionToLeaveWorkspaceException : Exception() {
    override val message: String
        get() = "워크스페이스를 탈퇴 할 권한이 없습니다."
}