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
        // 회원가입·계좌 등록 요청 스레드에서 동기로 불리고 OSIV로 DB 커넥션을 쥔 채 기다리므로,
        // 예금주 조회(토큰+조회 2회)가 프론트 타임아웃(10초) 안에 끝나도록 호출당 상한을 둔다.
        return OkHttpClient()
            .newBuilder().apply {
                connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                writeTimeout(CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                readTimeout(CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                callTimeout(CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
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

    companion object {
        const val CONNECT_TIMEOUT_SECONDS = 3L
        const val CALL_TIMEOUT_SECONDS = 4L
    }
}