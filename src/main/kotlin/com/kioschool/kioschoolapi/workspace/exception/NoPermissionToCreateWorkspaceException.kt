package com.kioschool.kioschoolapi.workspace.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(code = HttpStatus.UNAUTHORIZED, reason = "워크스페이스를 생성할 권한이 없습니다. 계좌정보를 등록해주세요.")
class NoPermissionToCreateWorkspaceException : Exception()