package com.kioschool.kioschoolapi.global.portone.dto

class GetTokenResponse(
    val access_token: String,
    val now: Long,
    val expired_at: Long,
)