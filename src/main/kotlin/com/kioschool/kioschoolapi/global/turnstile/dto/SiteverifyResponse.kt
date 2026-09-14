package com.kioschool.kioschoolapi.global.turnstile.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class SiteverifyResponse(
    val success: Boolean,
    @JsonProperty("error-codes")
    val errorCodes: List<String> = emptyList(),
)
