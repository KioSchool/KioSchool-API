package com.kioschool.kioschoolapi.domain.mailbox.controller

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.kioschool.kioschoolapi.domain.mailbox.listener.ResendWebhookEvent
import com.kioschool.kioschoolapi.domain.mailbox.listener.ResendWebhookHandler
import com.kioschool.kioschoolapi.global.resend.service.ResendWebhookVerifier
import io.swagger.v3.oas.annotations.Hidden
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController

@Hidden
@RestController
class ResendWebhookController(
    private val resendWebhookVerifier: ResendWebhookVerifier,
    private val resendWebhookHandler: ResendWebhookHandler,
    private val objectMapper: ObjectMapper,
    @Value("\${mail.inbox-id}")
    private val inboxId: String,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    // 서명은 받은 바이트 그대로 확인해야 하므로 객체로 바꾸기 전 원문을 받는다
    @PostMapping("/webhook/resend")
    fun receive(
        @RequestBody body: ByteArray,
        @RequestHeader("svix-id", required = false) svixId: String?,
        @RequestHeader("svix-timestamp", required = false) svixTimestamp: String?,
        @RequestHeader("svix-signature", required = false) svixSignature: String?,
    ): ResponseEntity<Void> {
        if (!resendWebhookVerifier.verify(body, svixId, svixTimestamp, svixSignature)) {
            return ResponseEntity.badRequest().build()
        }

        val event = parse(body) ?: return ResponseEntity.ok().build()
        if (event.inboxId == null || event.inboxId == inboxId) {
            resendWebhookHandler.handle(event)
        } else {
            // 설정의 메일함 ID가 틀리면 받은 메일이 모두 여기서 버려지므로 알아챌 수 있게 남긴다
            log.warn("Ignored Resend webhook from another inbox: type={}", event.type)
        }
        return ResponseEntity.ok().build()
    }

    private fun parse(body: ByteArray): ResendWebhookEvent? {
        val root = try {
            objectMapper.readTree(body)
        } catch (e: JsonProcessingException) {
            return null
        }
        val type = root?.path("type")?.textValue() ?: return null
        val data = root.path("data")
        return ResendWebhookEvent(
            type = type,
            inboxId = data.path("inbox_id").textValue(),
            threadId = data.path("thread_id").textValue(),
            emailId = data.path("email_id").textValue(),
        )
    }
}
