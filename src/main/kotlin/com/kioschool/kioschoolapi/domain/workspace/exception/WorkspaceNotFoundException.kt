package com.kioschool.kioschoolapi.domain.workspace.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(code = HttpStatus.NOT_FOUND, reason = "존재하지 않는 워크스페이스입니다.")
class WorkspaceNotFoundException : Exception()