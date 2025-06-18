package com.kioschool.kioschoolapi.domain.workspace.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus


@ResponseStatus(code = HttpStatus.FORBIDDEN, reason = "워크스페이스를 가입 할 권한이 없습니다.")
class NoPermissionToJoinWorkspaceException : Exception()