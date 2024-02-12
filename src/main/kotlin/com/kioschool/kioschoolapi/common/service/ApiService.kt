package com.kioschool.kioschoolapi.common.service

import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.Response
import org.springframework.stereotype.Service


@Service
class ApiService(
    private val okHttpClient: OkHttpClient
) {
    fun get(url: String, urlQuery: String = ""): Response {
        val requestUrl =
            if (urlQuery.isEmpty())
                url
            else if (url.contains("?") || urlQuery.contains("?"))
                "$url$urlQuery"
            else
                "$url?$urlQuery"

        val request = okhttp3.Request.Builder()
            .url(requestUrl)
            .build()

        return okHttpClient.newCall(request).execute()
    }

    fun post(url: String, body: RequestBody): Response {
        val request = okhttp3.Request.Builder()
            .url(url)
            .post(body)
            .build()

        return okHttpClient.newCall(request).execute()
    }
}