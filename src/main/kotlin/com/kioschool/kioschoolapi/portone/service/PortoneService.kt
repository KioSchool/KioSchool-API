package com.kioschool.kioschoolapi.portone.service

import com.kioschool.kioschoolapi.account.exception.IncorrectAccountHolderException
import com.kioschool.kioschoolapi.portone.api.PortoneApi
import com.kioschool.kioschoolapi.portone.dto.GetTokenRequest
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

    fun validateAccountHolder(bank: String, accountNumber: String, accountHolder: String) {
        val accessToken = getAccessToken()

        val response = portoneApi.getAccountHolder(accessToken, bank, accountNumber).execute()
        val realAccountHolder = response.body()?.response?.bank_holder ?: ""

        if (realAccountHolder != accountHolder) {
            throw IncorrectAccountHolderException()
        }
    }
}