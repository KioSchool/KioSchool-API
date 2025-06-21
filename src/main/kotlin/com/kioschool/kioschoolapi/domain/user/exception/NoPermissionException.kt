package com.kioschool.kioschoolapi.domain.user.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus


@ResponseStatus(code = HttpStatus.FORBIDDEN, reason = "권한이 없습니다.")
class NoPermissionException : Exception()