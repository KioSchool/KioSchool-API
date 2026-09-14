package com.kioschool.kioschoolapi.global.discord.dto

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class DiscordWebhookRequestTest : DescribeSpec({
    describe("직렬화") {
        it("모든 멘션 파싱을 끄는 allowed_mentions를 함께 보낸다") {
            val json = jacksonObjectMapper().writeValueAsString(DiscordWebhookRequest("@everyone 제목"))

            json shouldBe """{"content":"@everyone 제목","allowed_mentions":{"parse":[]}}"""
        }
    }
})
