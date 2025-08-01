package com.kioschool.kioschoolapi.global.portone

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "portone")
data class PortoneProperties(
    val apiKey: String,
    val apiSecret: String
)
