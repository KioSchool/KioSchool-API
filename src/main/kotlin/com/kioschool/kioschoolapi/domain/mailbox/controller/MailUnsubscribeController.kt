package com.kioschool.kioschoolapi.domain.mailbox.controller

import com.kioschool.kioschoolapi.domain.mailbox.service.MailUnsubscribeInfo
import com.kioschool.kioschoolapi.domain.mailbox.service.MailUnsubscribeService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Mail Unsubscribe Controller")
@RestController
class MailUnsubscribeController(
    private val mailUnsubscribeService: MailUnsubscribeService,
) {
    @Operation(summary = "수신 거부 확인", description = "조회만 하고 상태를 바꾸지 않습니다.")
    @GetMapping("/mail/unsubscribe")
    fun find(@RequestParam token: String): MailUnsubscribeInfo {
        return mailUnsubscribeService.find(token)
    }

    @Operation(summary = "수신 거부", description = "메일 프로그램의 원클릭 수신 거부(List-Unsubscribe-Post)도 이 경로로 들어옵니다.")
    @PostMapping("/mail/unsubscribe")
    fun unsubscribe(@RequestParam token: String) {
        mailUnsubscribeService.unsubscribe(token)
    }
}
