package com.kioschool.kioschoolapi.global.resend.service

import com.kioschool.kioschoolapi.global.resend.api.ResendInboxApi
import com.kioschool.kioschoolapi.global.resend.dto.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain
import io.mockk.*
import okhttp3.ResponseBody.Companion.toResponseBody
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder
import retrofit2.Call
import retrofit2.Response
import java.io.IOException

class ResendInboxClientTest : DescribeSpec({
    val api = mockk<ResendInboxApi>()
    val sut = ResendInboxClient("key", "inbox", api, MailRecipientGuard("*"), 0)

    afterTest { clearAllMocks() }

    fun <T> ok(body: T): Call<T> = mockk<Call<T>>().also { every { it.execute() } returns Response.success(body) }

    fun <T> fail(code: Int, body: String = "{}"): Call<T> = mockk<Call<T>>().also {
        every { it.execute() } returns Response.error(code, body.toResponseBody())
    }

    fun email(id: String) = ResendThreadEmail(
        id = id,
        direction = "inbound",
        from = "a@b.com",
        to = listOf("contact@kio-school.com"),
        subject = "s",
        html = null,
        text = "t",
        attachments = emptyList(),
        receivedAt = java.time.OffsetDateTime.parse("2026-11-01T00:00:00Z"),
    )

    describe("sendNew") {
        it("허용되지 않은 주소면 호출하지 않고 ResendSendException을 던진다") {
            val guarded = ResendInboxClient("key", "inbox", api, MailRecipientGuard("kio-school.com"), 0)

            shouldThrow<ResendSendException> {
                guarded.sendNew("a@konkuk.ac.kr", "s", "h", "t", emptyMap(), "k")
            }

            verify { api wasNot Called }
        }

        it("본인 주소로 보낼 때는 허용 도메인 밖이어도 보낸다") {
            val guarded = ResendInboxClient("key", "inbox", api, MailRecipientGuard("kio-school.com"), 0)
            every { api.createDraft(any(), "k-draft", "inbox", any()) } returns ok(ResendIdResponse("draft"))
            every { api.sendDraft(any(), "k-send", "inbox", "draft", any()) } returns ok(ResendIdResponse("email"))

            guarded.sendToOwnAddress("me@gmail.com", "s", "h", "t", emptyMap(), "k") shouldBe "email"
        }

        it("초안 생성과 발송에 각각 다른 Idempotency-Key를 쓴다") {
            every { api.createDraft(any(), "k-draft", "inbox", any()) } returns ok(ResendIdResponse("draft"))
            every { api.sendDraft(any(), "k-send", "inbox", "draft", any()) } returns ok(ResendIdResponse("email"))

            sut.sendNew("a@b.com", "s", "h", "t", emptyMap(), "k") shouldBe "email"

            verify(exactly = 1) { api.createDraft("Bearer key", "k-draft", "inbox", any()) }
            verify(exactly = 1) { api.sendDraft("Bearer key", "k-send", "inbox", "draft", any()) }
        }

        it("제목의 줄바꿈을 공백으로 바꾼다") {
            val body = slot<ResendCreateDraftRequest>()
            every { api.createDraft(any(), any(), any(), capture(body)) } returns ok(ResendIdResponse("draft"))
            every { api.sendDraft(any(), any(), any(), any(), any()) } returns ok(ResendIdResponse("email"))

            sut.sendNew("a@b.com", "제목\r\n둘째 줄", "h", "t", emptyMap(), "k")

            body.captured.subject shouldBe "제목 둘째 줄"
        }

        it("헤더를 발송 요청에 넘긴다") {
            val body = slot<ResendSendDraftRequest>()
            every { api.createDraft(any(), any(), any(), any()) } returns ok(ResendIdResponse("draft"))
            every { api.sendDraft(any(), any(), any(), any(), capture(body)) } returns ok(ResendIdResponse("email"))

            sut.sendNew("a@b.com", "s", "h", "t", mapOf("List-Unsubscribe" to "<u>"), "k")

            body.captured.headers shouldBe mapOf("List-Unsubscribe" to "<u>")
        }

        listOf("daily_quota_exceeded", "monthly_quota_exceeded").forEach { name ->
            it("429 $name 이면 ResendDailyLimitException을 던진다") {
                every { api.createDraft(any(), any(), any(), any()) } returns fail(429, """{"statusCode":429,"name":"$name"}""")

                shouldThrow<ResendDailyLimitException> {
                    sut.sendNew("a@b.com", "s", "h", "t", emptyMap(), "k")
                }
            }
        }

        it("429 rate_limit_exceeded면 잠시 뒤 다시 시도해 성공한다") {
            val limited = fail<ResendIdResponse>(429, """{"name":"rate_limit_exceeded"}""")
            every { limited.clone() } returns ok(ResendIdResponse("draft"))
            every { api.createDraft(any(), any(), any(), any()) } returns limited
            every { api.sendDraft(any(), any(), any(), any(), any()) } returns ok(ResendIdResponse("email"))

            sut.sendNew("a@b.com", "s", "h", "t", emptyMap(), "k") shouldBe "email"
        }

        it("rate_limit_exceeded가 계속되면 한도 초과가 아니라 ResendSendException이다") {
            val limited = fail<ResendIdResponse>(429, """{"name":"rate_limit_exceeded"}""")
            every { limited.clone() } returns limited
            every { api.createDraft(any(), any(), any(), any()) } returns limited

            shouldThrow<ResendSendException> {
                sut.sendNew("a@b.com", "s", "h", "t", emptyMap(), "k")
            }
        }

        it("5xx를 받으면 ResendSendException을 던지고 메시지에 받는 주소를 넣지 않는다") {
            every { api.createDraft(any(), any(), any(), any()) } returns ok(ResendIdResponse("draft"))
            every { api.sendDraft(any(), any(), any(), any(), any()) } returns fail(500)

            val thrown = shouldThrow<ResendSendException> {
                sut.sendNew("secret@b.com", "s", "h", "t", emptyMap(), "k")
            }

            thrown.message.orEmpty() shouldNotContain "secret"
        }

        it("네트워크 오류도 ResendSendException으로 바꾼다") {
            val call = mockk<Call<ResendIdResponse>>()
            every { call.execute() } throws IOException("timeout")
            every { api.createDraft(any(), any(), any(), any()) } returns call

            shouldThrow<ResendSendException> {
                sut.sendNew("a@b.com", "s", "h", "t", emptyMap(), "k")
            }
        }
    }

    describe("reply") {
        it("스레드와 메일을 지정해 답장하고 이메일 ID를 돌려준다") {
            every { api.reply("Bearer key", "k", "inbox", "thread", "email", any()) } returns ok(ResendIdResponse("new"))

            sut.reply("thread", "email", "h", "t", "k") shouldBe "new"
        }
    }

    describe("listThreadEmails") {
        it("has_more가 true면 마지막 id를 after로 넘겨 끝까지 가져온다") {
            every { api.listThreadEmails(any(), "inbox", "thread", 100, null) } returns
                ok(ResendListResponse(hasMore = true, data = listOf(email("1"), email("2"))))
            every { api.listThreadEmails(any(), "inbox", "thread", 100, "2") } returns
                ok(ResendListResponse(hasMore = false, data = listOf(email("3"))))

            sut.listThreadEmails("thread").map { it.id } shouldBe listOf("1", "2", "3")
        }

        val mapper = Jackson2ObjectMapperBuilder.json().build<com.fasterxml.jackson.databind.ObjectMapper>()

        it("attachments·to가 없거나 null이어도 읽는다") {
            val parsed = mapper.readValue(
                """{"id":"m","direction":"inbound","from":"a@b.com","to":null,"received_at":"2026-08-05T14:03:11.229Z"}""",
                ResendThreadEmail::class.java,
            )

            parsed.to shouldBe emptyList()
            parsed.attachments shouldBe emptyList()
        }

        it("received_at 대신 created_at이 와도 읽고 공백 형식 시각도 읽는다") {
            val parsed = mapper.readValue(
                """{"id":"m","direction":"outbound","from":"a@b.com","to":["c@d.com"],"created_at":"2026-08-05 14:03:11.229+00"}""",
                ResendThreadEmail::class.java,
            )

            parsed.receivedAt.toInstant().toString() shouldBe "2026-08-05T14:03:11.229Z"
        }

        it("문서의 응답 예시를 읽는다") {
            val json = """
                {"object":"list","has_more":false,"data":[{
                  "id":"m1","direction":"inbound","from":"Ada <ada@example.org>",
                  "to":["support@example.com"],"cc":[],"bcc":[],"reply_to":["r@example.org"],
                  "subject":null,"message_id":"<1@x>","html":null,"text":"본문",
                  "attachments":[{"id":"a1","filename":"receipt.pdf","size":20841},{"id":"a2","filename":null,"size":null}],
                  "read":false,"received_at":"2026-08-05T14:03:11.229Z"}]}
            """.trimIndent()
            val type = mapper.typeFactory.constructParametricType(ResendListResponse::class.java, ResendThreadEmail::class.java)

            val parsed = mapper.readValue<ResendListResponse<ResendThreadEmail>>(json, type)

            parsed.hasMore shouldBe false
            parsed.data shouldHaveSize 1
            parsed.data[0].from shouldBe "Ada <ada@example.org>"
            parsed.data[0].subject.shouldBeNull()
            parsed.data[0].attachments[0].filename shouldBe "receipt.pdf"
            parsed.data[0].attachments[1].size.shouldBeNull()
            parsed.data[0].receivedAt.toInstant().toString() shouldBe "2026-08-05T14:03:11.229Z"
        }
    }

    describe("deleteThread") {
        it("404는 예외를 던지지 않는다") {
            every { api.deleteThread(any(), "inbox", "gone") } returns fail(404)

            sut.deleteThread("gone")
        }

        it("그 밖의 실패는 ResendSendException을 던진다") {
            every { api.deleteThread(any(), "inbox", "t") } returns fail(500)

            shouldThrow<ResendSendException> { sut.deleteThread("t") }
        }
    }

    describe("attachmentDownloadUrl") {
        it("임시 URL을 돌려준다") {
            every { api.getReceivedAttachment(any(), "email", "att") } returns ok(ResendAttachmentDetail("https://cdn/x"))

            sut.attachmentDownloadUrl("email", "att") shouldBe "https://cdn/x"
        }

        it("404면 null을 돌려준다") {
            every { api.getReceivedAttachment(any(), "email", "att") } returns fail(404)

            sut.attachmentDownloadUrl("email", "att").shouldBeNull()
        }
    }
})
