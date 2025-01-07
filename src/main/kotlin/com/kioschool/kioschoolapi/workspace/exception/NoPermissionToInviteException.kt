package com.kioschool.kioschoolapi.workspace.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(code = HttpStatus.FORBIDDEN, reason = "초대 권한이 없습니다.")
class NoPermissionToInviteException : Exception()