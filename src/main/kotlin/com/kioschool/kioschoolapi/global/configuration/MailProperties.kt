package com.kioschool.kioschoolapi.global.configuration

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "spring.mail")
data class MailProperties(
    val host: String,
    val port: Int,
    val username: String,
    val password: String,
    val properties: Mail, 
) {
    data class Mail(
        val smtp: Smtp
    )
    data class Smtp(
        val auth: Boolean,
        val starttls: StartTls
    )
    data class StartTls(
        val enable: Boolean
    )
}
