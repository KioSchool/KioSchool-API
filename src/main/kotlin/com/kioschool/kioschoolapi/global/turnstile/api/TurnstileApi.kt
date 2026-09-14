package com.kioschool.kioschoolapi.global.turnstile.api

import com.kioschool.kioschoolapi.global.turnstile.dto.SiteverifyResponse
import retrofit2.Call
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface TurnstileApi {
    @FormUrlEncoded
    @POST("turnstile/v0/siteverify")
    fun siteverify(
        @Field("secret") secret: String,
        @Field("response") token: String
    ): Call<SiteverifyResponse>
}
