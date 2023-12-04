package com.kioschool.kioschoolapi.workspace.exception

class NoInvitationToJoinWorkspaceException : Exception() {
    override val message: String
        get() = "초대 받지 않은 워크스페이스입니다."
}