package com.kioschool.kioschoolapi.global.resend.service

import okhttp3.OkHttpClient
import okhttp3.Request
import org.springframework.stereotype.Component
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.TimeUnit

@Component
class ResendAttachmentDownloader(
    okHttpClient: OkHttpClient,
) {
    // 첨부는 수십 MB까지 올 수 있어 공용 호출 상한(4초)을 쓰지 않는다
    private val client = okHttpClient.newBuilder()
        .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .callTimeout(CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    /** 임시 URL이 만료되는 등 파일을 받지 못하면 false를 돌려주고, [write]를 부르지 않는다. */
    fun download(url: String, write: (InputStream) -> Unit): Boolean {
        val request = Request.Builder().url(url).build()
        try {
            client.newCall(request).execute().use { response ->
                val body = response.body
                if (!response.isSuccessful || body == null) return false
                write(body.byteStream())
                return true
            }
        } catch (e: IOException) {
            throw ResendSendException("Resend 첨부 내려받기 실패", e)
        }
    }

    companion object {
        private const val READ_TIMEOUT_SECONDS = 30L
        private const val CALL_TIMEOUT_SECONDS = 120L
    }
}
