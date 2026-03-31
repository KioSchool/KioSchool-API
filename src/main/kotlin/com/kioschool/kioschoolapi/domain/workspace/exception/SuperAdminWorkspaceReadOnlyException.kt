package com.kioschool.kioschoolapi.domain.workspace.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(code = HttpStatus.BAD_REQUEST, reason = "최고 관리자(Super Admin)는 타인의 워크스페이스를 조회만 할 수 있으며, 수정(CUD)은 불가능합니다.")
class SuperAdminWorkspaceReadOnlyException : Exception()
