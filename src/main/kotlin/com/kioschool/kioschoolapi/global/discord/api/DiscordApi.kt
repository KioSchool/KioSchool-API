package com.kioschool.kioschoolapi.global.discord.api

import com.kioschool.kioschoolapi.global.discord.dto.DiscordWebhookRequest
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Url

interface DiscordApi {
    @POST
    fun executeWebhook(
        @Url webhookUrl: String,
        @Body request: DiscordWebhookRequest
    ): Call<Void>
}
