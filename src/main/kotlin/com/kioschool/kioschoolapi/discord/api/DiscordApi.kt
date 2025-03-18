package com.kioschool.kioschoolapi.discord.api

import okhttp3.FormBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Url

interface DiscordApi {
    @POST
    fun executeWebhook(
        @Url webhookUrl: String,
        @Body request: FormBody
    ): Call<Void>
}