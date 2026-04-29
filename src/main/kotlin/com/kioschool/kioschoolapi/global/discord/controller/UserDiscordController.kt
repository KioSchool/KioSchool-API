package com.kioschool.kioschoolapi.global.discord.controller

import com.kioschool.kioschoolapi.global.discord.dto.SendPopupResultRequestBody
import com.kioschool.kioschoolapi.global.discord.service.DiscordService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@Tag(name = "User Discord Controller")
@RestController
class UserDiscordController(
    private val discordService: DiscordService,
) {
    @Operation(summary = "익명 팝업 결과 전송", description = "회원가입이나 로그인 없이 팝업 결과를 디스코드 채널로 전송합니다.")
    @PostMapping("/discord/popup")
    fun sendPopupResult(
        @RequestBody body: SendPopupResultRequestBody
    ) {
        return discordService.sendPopupResult(
            "익명 사용자",
            body.result
        )
    }
}
