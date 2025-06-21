package com.kioschool.kioschoolapi.global.discord.service

import com.kioschool.kioschoolapi.domain.user.entity.User
import com.kioschool.kioschoolapi.domain.workspace.entity.Workspace
import com.kioschool.kioschoolapi.global.discord.api.DiscordApi
import okhttp3.FormBody
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class DiscordService(
    @Value("\${discord.webhook-url}")
    private val webhookUrl: String,
    private val discordApi: DiscordApi
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

    fun sendWorkspaceCreate(workspace: Workspace) {
        val message =
            """## [워크스페이스 생성]
            |워크스페이스 ID: ${workspace.id}
            |워크스페이스 이름: ${workspace.name}
            |워크스페이스 생성자: ${workspace.owner.name}
            """.trimMargin()

        send(message)
    }

    fun sendPopupResult(username: String, result: String) {
        val message = """## [팝업 결과]
        |아이디: $username
        $result
        """.trimMargin()

        send(message)
    }

    private fun send(message: String) {
        val body = FormBody.Builder()
            .add("content", message)
            .build()

        discordApi.executeWebhook(webhookUrl, body).execute()
    }
}