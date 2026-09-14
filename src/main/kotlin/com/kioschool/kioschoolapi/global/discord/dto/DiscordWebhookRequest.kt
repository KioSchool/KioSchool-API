package com.kioschool.kioschoolapi.global.discord.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class DiscordWebhookRequest(
    val content: String,
) {
    // 문의 제목 등 외부 입력이 메시지에 섞이므로 @everyone·역할 멘션이 울리지 않게 모든 멘션 파싱을 끈다
    @get:JsonProperty("allowed_mentions")
    val allowedMentions: Map<String, List<String>> = mapOf("parse" to emptyList())
}
