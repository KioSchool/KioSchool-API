package com.kioschool.kioschoolapi.domain.mailbox.dto

import com.fasterxml.jackson.databind.ObjectMapper
import com.kioschool.kioschoolapi.domain.mailbox.dto.common.MailAttachmentDto
import com.kioschool.kioschoolapi.domain.mailbox.dto.common.MailMessageDto
import com.kioschool.kioschoolapi.domain.mailbox.dto.common.MailThreadDetailDto
import com.kioschool.kioschoolapi.domain.mailbox.dto.request.ReplyMailRequestBody
import com.kioschool.kioschoolapi.domain.mailbox.dto.request.SendMailRequestBody
import com.kioschool.kioschoolapi.domain.mailbox.enum.MailDirection
import com.kioschool.kioschoolapi.domain.mailbox.enum.MailThreadStatus
import com.kioschool.kioschoolapi.global.logging.util.LogMasking
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.string.shouldNotContain
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder
import java.time.LocalDateTime

class MailDtoLogMaskingTest : DescribeSpec({
    val mapper = LogMasking.loggingMapper(Jackson2ObjectMapperBuilder.json().build<ObjectMapper>())
    val at = LocalDateTime.of(2026, 11, 1, 12, 0)

    describe("로그 직렬화") {
        it("대화 응답에 메일 내용과 개인정보가 남지 않는다") {
            val logged = mapper.writeValueAsString(
                MailThreadDetailDto(
                    id = 1,
                    subject = "비밀 제목",
                    status = MailThreadStatus.NEEDS_REPLY,
                    isUnread = true,
                    workspaceId = 2,
                    workspaceName = "주점",
                    counterpartName = "비밀 이름",
                    counterpartEmail = "secret@test.com",
                    lastMessageAt = at,
                    schoolName = "건국대학교",
                    messages = listOf(
                        MailMessageDto(
                            id = 3,
                            direction = MailDirection.INBOUND,
                            fromName = "비밀 이름",
                            fromEmail = "secret@test.com",
                            sentByName = "비밀 팀원",
                            textBody = "비밀 본문",
                            htmlBody = "<p>비밀 본문</p>",
                            sentAt = at,
                            bounced = false,
                            attachments = listOf(MailAttachmentDto(4, "비밀 통장.pdf", 10)),
                        )
                    ),
                )
            )

            logged shouldNotContain "비밀"
            logged shouldNotContain "secret@"
        }

        it("요청 본문의 제목과 내용이 남지 않는다") {
            val logged = mapper.writeValueAsString(SendMailRequestBody(1, "비밀 제목", "비밀 내용")) +
                mapper.writeValueAsString(ReplyMailRequestBody(1, "비밀 내용"))

            logged shouldNotContain "비밀"
        }
    }
})
