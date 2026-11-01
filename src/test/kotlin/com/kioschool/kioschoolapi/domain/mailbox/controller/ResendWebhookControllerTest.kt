package com.kioschool.kioschoolapi.domain.mailbox.controller

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.fasterxml.jackson.databind.ObjectMapper
import com.kioschool.kioschoolapi.domain.mailbox.listener.ResendWebhookEvent
import com.kioschool.kioschoolapi.domain.mailbox.listener.ResendWebhookHandler
import com.kioschool.kioschoolapi.global.resend.service.ResendWebhookVerifier
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.*
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.setup.MockMvcBuilders

class ResendWebhookControllerTest : DescribeSpec({
    val verifier = mockk<ResendWebhookVerifier>()
    val handler = mockk<ResendWebhookHandler>(relaxed = true)
    val mvc = MockMvcBuilders
        .standaloneSetup(ResendWebhookController(verifier, handler, ObjectMapper(), "inbox-1"))
        .build()

    afterTest { clearAllMocks() }

    fun body(inboxId: String? = "inbox-1") = """
        {"type":"inbox.email.received","data":{${inboxId?.let { "\"inbox_id\":\"$it\"," } ?: ""}"thread_id":"t1","email_id":"e1"}}
    """.trimIndent()

    fun post(json: String) = mvc.post("/webhook/resend") {
        contentType = MediaType.APPLICATION_JSON
        content = json
        header("svix-id", "msg_1")
        header("svix-timestamp", "1")
        header("svix-signature", "v1,x")
    }

    describe("POST /webhook/resend") {
        it("서명이 틀리면 400이고 핸들러를 부르지 않는다") {
            every { verifier.verify(any(), any(), any(), any(), any()) } returns false

            post(body()).andExpect { status { isBadRequest() } }

            verify { handler wasNot Called }
        }

        it("받은 원문 그대로 서명을 확인한다") {
            val json = body()
            val bytes = slot<ByteArray>()
            every { verifier.verify(capture(bytes), "msg_1", "1", "v1,x", any()) } returns true

            post(json).andExpect { status { isOk() } }

            String(bytes.captured) == json || throw AssertionError("원문이 달라요")
        }

        it("서명이 맞으면 200이고 핸들러를 한 번 부른다") {
            every { verifier.verify(any(), any(), any(), any(), any()) } returns true

            post(body()).andExpect { status { isOk() } }

            verify(exactly = 1) {
                handler.handle(ResendWebhookEvent("inbox.email.received", "inbox-1", "t1", "e1"))
            }
        }

        it("inbox_id가 설정값과 다르면 200이지만 핸들러를 부르지 않고 경고를 남긴다") {
            val appender = ListAppender<ILoggingEvent>().apply { start() }
            (LoggerFactory.getLogger(ResendWebhookController::class.java) as Logger).addAppender(appender)
            every { verifier.verify(any(), any(), any(), any(), any()) } returns true

            post(body("other")).andExpect { status { isOk() } }

            verify { handler wasNot Called }
            appender.list.any { it.level == Level.WARN && it.formattedMessage.contains("inbox.email.received") } shouldBe true
        }

        it("inbox_id가 없는 이벤트(email.bounced)도 핸들러를 부른다") {
            every { verifier.verify(any(), any(), any(), any(), any()) } returns true

            post("""{"type":"email.bounced","data":{"email_id":"e1"}}""").andExpect { status { isOk() } }

            verify(exactly = 1) { handler.handle(ResendWebhookEvent("email.bounced", null, null, "e1")) }
        }

        it("서명이 맞아도 JSON이 아니면 200이고 핸들러를 부르지 않는다") {
            every { verifier.verify(any(), any(), any(), any(), any()) } returns true

            post("not json").andExpect { status { isOk() } }

            verify { handler wasNot Called }
        }
    }
})
