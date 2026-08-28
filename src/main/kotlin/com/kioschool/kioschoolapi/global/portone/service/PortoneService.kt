package com.kioschool.kioschoolapi.global.portone.service

import com.kioschool.kioschoolapi.global.error.ErrorCode
import com.kioschool.kioschoolapi.global.error.exception.CustomException
import com.kioschool.kioschoolapi.global.portone.api.PortoneApi
import com.kioschool.kioschoolapi.global.portone.dto.GetTokenRequest
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class PortoneService(
    @Value("\${portone.api-key}")
    private val apiKey: String,
    @Value("\${portone.api-secret}")
    private val apiSecret: String,
    private val portoneApi: PortoneApi
) {
    private fun getAccessToken(): String {
        val response = portoneApi.getToken(GetTokenRequest(apiKey, apiSecret)).execute()
        return "Bearer ${response.body()?.response?.access_token ?: ""}"
    }

    fun getAccountHolder(bank: String, accountNumber: String): String {
        val accessToken = getAccessToken()

        val response = portoneApi.getAccountHolder(accessToken, bank, accountNumber).execute()
        val body = response.body()
        val accountHolder = body?.response?.bank_holder?.trim()

        if (body == null || body.code != 0 || accountHolder.isNullOrBlank()) {
            throw CustomException(ErrorCode.ACCOUNT_HOLDER_NOT_FOUND)
        }

        return accountHolder
    }

    fun validateAccountHolder(bank: String, accountNumber: String, accountHolder: String) {
        val accessToken = getAccessToken()

        val response = portoneApi.getAccountHolder(accessToken, bank, accountNumber).execute()
        val body = response.body()
        val realAccountHolder = body?.response?.bank_holder

        if (body == null || body.code != 0 || realAccountHolder.isNullOrBlank()) {
            throw CustomException(ErrorCode.INCORRECT_ACCOUNT_HOLDER)
        }

        if (!accountHolder.startsWith(realAccountHolder)) {
            throw CustomException(ErrorCode.INCORRECT_ACCOUNT_HOLDER)
        }
    }
}