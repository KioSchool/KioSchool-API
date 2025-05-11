package com.kioschool.kioschoolapi.discord.controller

import com.kioschool.kioschoolapi.common.annotation.AdminUsername
import com.kioschool.kioschoolapi.discord.dto.SendPopupResultRequestBody
import com.kioschool.kioschoolapi.discord.service.DiscordService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Admin Discord Controller")
@RestController
@RequestMapping("/admin")
class AdminDiscordController(
    private val discordService: DiscordService,
) {
    @Operation(summary = "팝업 결과 전송", description = "팝업 결과를 전송합니다. 디스코드 채널로 전송됩니다.")
    @PostMapping("/discord/popup")
    fun sendPopupResult(
        @AdminUsername username: String,
        @RequestBody body: SendPopupResultRequestBody
    ) {
        return discordService.sendPopupResult(
            username,
            body.result
        )
    }
}