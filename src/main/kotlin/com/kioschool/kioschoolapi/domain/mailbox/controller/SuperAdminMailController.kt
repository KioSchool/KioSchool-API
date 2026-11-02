package com.kioschool.kioschoolapi.domain.mailbox.controller

import com.kioschool.kioschoolapi.domain.mailbox.dto.common.MailPreviewDto
import com.kioschool.kioschoolapi.domain.mailbox.dto.common.MailThreadDetailDto
import com.kioschool.kioschoolapi.domain.mailbox.dto.common.MailThreadSummaryDto
import com.kioschool.kioschoolapi.domain.mailbox.dto.request.MailPreviewRequestBody
import com.kioschool.kioschoolapi.domain.mailbox.dto.request.ReplyMailRequestBody
import com.kioschool.kioschoolapi.domain.mailbox.dto.request.SendMailRequestBody
import com.kioschool.kioschoolapi.domain.mailbox.enum.MailThreadStatus
import com.kioschool.kioschoolapi.domain.mailbox.facade.MailFacade
import com.kioschool.kioschoolapi.global.security.annotation.AdminUsername
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@Tag(name = "Super Admin Mail Controller")
@RestController
@RequestMapping("/super-admin/mail")
class SuperAdminMailController(
    private val mailFacade: MailFacade,
) {
    @Operation(summary = "메일함 목록 조회", description = "대화를 최근 메일 순으로 조회합니다. 주점을 지정하면 그 주점의 대화를 모두 보여줍니다.")
    @GetMapping("/threads")
    fun getThreads(
        @RequestParam(required = false) status: MailThreadStatus?,
        @RequestParam(required = false) workspaceId: Long?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): Page<MailThreadSummaryDto> {
        return mailFacade.getThreads(status, workspaceId, page, size)
    }

    @Operation(summary = "대화 조회", description = "주고받은 메일을 조회하고 읽음으로 표시합니다.")
    @GetMapping("/threads/{threadId}")
    fun getThread(@PathVariable threadId: Long): MailThreadDetailDto {
        return mailFacade.getThread(threadId)
    }

    @Operation(summary = "새 메일 발송", description = "주점 사장 이메일로 새 메일을 보냅니다.")
    @PostMapping("/threads")
    @ResponseStatus(HttpStatus.ACCEPTED)
    fun sendNew(
        @AdminUsername username: String,
        @RequestBody @Valid body: SendMailRequestBody,
    ) {
        mailFacade.sendNew(username, body)
    }

    @Operation(summary = "답장 발송", description = "그사이 새 메일이 왔으면 보내지 않고 409를 돌려줍니다.")
    @PostMapping("/threads/{threadId}/reply")
    fun reply(
        @AdminUsername username: String,
        @PathVariable threadId: Long,
        @RequestBody @Valid body: ReplyMailRequestBody,
    ): MailThreadDetailDto {
        return mailFacade.reply(username, threadId, body)
    }

    @Operation(summary = "메일 미리보기", description = "실제 메일과 같은 HTML을 만듭니다.")
    @PostMapping("/preview")
    fun preview(@RequestBody @Valid body: MailPreviewRequestBody): MailPreviewDto {
        return mailFacade.preview(body)
    }

    @Operation(summary = "답장 없이 완료", description = "새 메일이 오면 다시 답장 필요로 돌아갑니다.")
    @PostMapping("/threads/{threadId}/resolve")
    fun resolve(@PathVariable threadId: Long): MailThreadDetailDto {
        return mailFacade.resolve(threadId)
    }

    @Operation(summary = "스팸 표시")
    @PostMapping("/threads/{threadId}/spam")
    fun markSpam(@PathVariable threadId: Long): MailThreadDetailDto {
        return mailFacade.markSpam(threadId)
    }

    @Operation(summary = "스팸 표시 취소")
    @DeleteMapping("/threads/{threadId}/spam")
    fun unmarkSpam(@PathVariable threadId: Long): MailThreadDetailDto {
        return mailFacade.unmarkSpam(threadId)
    }

    @Operation(summary = "대화 삭제", description = "개인정보 삭제 요청을 처리할 때 씁니다. Resend에서도 함께 지웁니다.")
    @DeleteMapping("/threads/{threadId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable threadId: Long) {
        mailFacade.delete(threadId)
    }

    @Operation(summary = "받은 메일 첨부 내려받기")
    @GetMapping("/attachments/{attachmentId}/download")
    fun downloadAttachment(
        @PathVariable attachmentId: Long,
        response: HttpServletResponse,
    ) {
        mailFacade.downloadAttachment(attachmentId, response)
    }
}
