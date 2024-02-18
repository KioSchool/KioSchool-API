package com.kioschool.kioschoolapi.discord

import com.kioschool.kioschoolapi.common.service.ApiService
import com.kioschool.kioschoolapi.user.entity.User
import okhttp3.FormBody
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class DiscordService(
    @Value("\${discord.webhook-url}")
    private val webhookUrl: String,
    private val apiService: ApiService
) {
    fun sendUserRegister(user: User) {
        val message =
            """## [회원가입]
            |아이디: ${user.loginId}
            |이름: ${user.name}
            |이메일: ${user.email}
            """.trimMargin()

        send(message)
    }


    private fun send(message: String) {
        val body = FormBody.Builder()
            .add("content", message)
            .build()

        apiService.post(webhookUrl, body)
    }
}