package com.kioschool.kioschoolapi.domain.workspace.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(code = HttpStatus.BAD_REQUEST, reason = "접근 불가능한 워크스페이스입니다.")
class WorkspaceInaccessibleException : Exception()