package com.kioschool.kioschoolapi.workspace.exception

class WorkspaceInaccessibleException : Exception() {
    override val message: String
        get() = "접근 불가능한 워크스페이스입니다."
}