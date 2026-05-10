package com.kioschool.kioschoolapi.global.discord.service

import com.kioschool.kioschoolapi.domain.user.entity.User
import com.kioschool.kioschoolapi.domain.workspace.entity.Workspace
import com.kioschool.kioschoolapi.global.discord.api.DiscordApi
import okhttp3.FormBody
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service

@Service
class DiscordService(
    @Value("\${discord.webhook-url}")
    private val webhookUrl: String,
    private val discordApi: DiscordApi
) {
    @Async
    fun sendUserRegister(user: User) {
        val message =
            """## [회원가입]
            |아이디: ${user.loginId}
            |이름: ${user.name}
            |이메일: ${user.email}
            """.trimMargin()

        send(message)
    }

    @Async
    fun sendWorkspaceCreate(workspace: Workspace) {
        val message =
            """## [워크스페이스 생성]
            |워크스페이스 ID: ${workspace.id}
            |워크스페이스 이름: ${workspace.name}
            |워크스페이스 생성자: ${workspace.owner.name}
            """.trimMargin()

        send(message)
    }

    @Async
    fun sendPopupResult(sender: String, result: String) {
        val message = """## [팝업 결과]
        |전송자: $sender
        $result
        """.trimMargin()

        send(message)
    }

    @Async
    fun sendInsightCardSummary(
        referenceDate: java.time.LocalDate,
        successCount: Int,
        failedWorkspaceIds: List<Long>
    ) {
        val failedSection = if (failedWorkspaceIds.isEmpty()) "없음"
        else failedWorkspaceIds.joinToString(", ")

        val message =
            """## [데일리 인사이트 카드 생성]
            |기준일: $referenceDate
            |성공: ${successCount}건
            |실패 워크스페이스: $failedSection
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
