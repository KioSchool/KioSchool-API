package com.kioschool.kioschoolapi.global.configuration

import com.fasterxml.jackson.databind.ObjectMapper
import com.kioschool.kioschoolapi.global.discord.api.DiscordApi
import com.kioschool.kioschoolapi.global.portone.api.PortoneApi
import com.kioschool.kioschoolapi.global.turnstile.api.TurnstileApi
import okhttp3.OkHttpClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import java.util.concurrent.TimeUnit

@Configuration
class RetrofitConfiguration {
    @Bean("okHttpClient")
    fun okHttpClient(): OkHttpClient {
        return OkHttpClient()
            .newBuilder().apply {
                connectTimeout(15, TimeUnit.SECONDS)
                writeTimeout(15, TimeUnit.SECONDS)
                readTimeout(15, TimeUnit.SECONDS)
                callTimeout(15, TimeUnit.SECONDS)
            }.build()
    }

    @Bean("discordApi")
    fun discordApi(
        okHttpClient: OkHttpClient,
        objectMapper: ObjectMapper
    ): DiscordApi {
        return Retrofit.Builder()
            .baseUrl("https://discord.com/api/")
            .client(okHttpClient)
            .addConverterFactory(JacksonConverterFactory.create(objectMapper))
            .build()
            .create(DiscordApi::class.java)
    }

    @Bean("portoneApi")
    fun portoneApi(
        okHttpClient: OkHttpClient,
        objectMapper: ObjectMapper
    ): PortoneApi {
        return Retrofit.Builder()
            .baseUrl("https://api.iamport.kr/")
            .client(okHttpClient)
            .addConverterFactory(JacksonConverterFactory.create(objectMapper))
            .build()
            .create(PortoneApi::class.java)
    }

    @Bean("turnstileApi")
    fun turnstileApi(
        okHttpClient: OkHttpClient,
        objectMapper: ObjectMapper
    ): TurnstileApi {
        return Retrofit.Builder()
            .baseUrl("https://challenges.cloudflare.com/")
            .client(okHttpClient)
            .addConverterFactory(JacksonConverterFactory.create(objectMapper))
            .build()
            .create(TurnstileApi::class.java)
    }
}