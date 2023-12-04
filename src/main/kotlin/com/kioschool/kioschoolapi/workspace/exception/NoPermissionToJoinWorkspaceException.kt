package com.kioschool.kioschoolapi.workspace.exception

class NoPermissionToJoinWorkspaceException : Exception() {
    override val message: String
        get() = "워크스페이스를 가입 할 권한이 없습니다."
}