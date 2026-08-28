package com.kioschool.kioschoolapi.domain.account.dto.request

class RegisterAccountRequestBody(
    val bankId: Long,
    val accountNumber: String,
    @Deprecated("예금주명은 PortOne 조회값을 사용한다. 구버전 FE 호환을 위해 남겨둔 필드이며 무시된다.")
    val accountHolder: String? = null,
)
