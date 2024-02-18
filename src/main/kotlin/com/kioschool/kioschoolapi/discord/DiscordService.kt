package com.kioschool.kioschoolapi.discord

import com.kioschool.kioschoolapi.common.service.ApiService
import com.kioschool.kioschoolapi.user.entity.User
import com.kioschool.kioschoolapi.workspace.entity.Workspace
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

    fun sendWorkspaceCreate(workspace: Workspace) {
        val message =
            """## [워크스페이스 생성]
            |워크스페이스 ID: ${workspace.id}
            |워크스페이스 이름: ${workspace.name}
            |워크스페이스 생성자: ${workspace.owner.name}
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