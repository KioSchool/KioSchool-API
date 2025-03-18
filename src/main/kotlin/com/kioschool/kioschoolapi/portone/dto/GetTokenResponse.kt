package com.kioschool.kioschoolapi.portone.dto

class GetTokenResponse(
    val access_token: String,
    val now: Long,
    val expired_at: Long,
)