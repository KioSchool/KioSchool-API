package com.kioschool.kioschoolapi.discord.controller

import com.kioschool.kioschoolapi.common.annotation.AdminUsername
import com.kioschool.kioschoolapi.discord.dto.SendConfirmResultRequestBody
import com.kioschool.kioschoolapi.discord.service.DiscordService
import io.swagger.v3.oas.annotations.Operation
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class DiscordController(
    private val discordService: DiscordService,
) {
    @Operation(summary = "컨펌 결과 전송", description = "컨펌 결과를 전송합니다. 디스코드 채널로 전송됩니다.")
    @PostMapping("/discord/confirm")
    fun sendConfirmResult(
        @AdminUsername adminUsername: String,
        @RequestBody body: SendConfirmResultRequestBody
    ) {
        return discordService.sendConfirmResult(
            body.result
        )
    }
}