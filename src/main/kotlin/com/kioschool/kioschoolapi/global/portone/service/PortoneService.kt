package com.kioschool.kioschoolapi.global.portone.service

import com.kioschool.kioschoolapi.domain.account.exception.IncorrectAccountHolderException
import com.kioschool.kioschoolapi.global.portone.PortoneProperties
import com.kioschool.kioschoolapi.global.portone.api.PortoneApi
import com.kioschool.kioschoolapi.global.portone.dto.GetTokenRequest
import org.springframework.stereotype.Service

@Service
class PortoneService(
    private val portoneProperties: PortoneProperties,
    private val portoneApi: PortoneApi
) {
    private fun getAccessToken(): String {
        val response = portoneApi.getToken(GetTokenRequest(portoneProperties.apiKey, portoneProperties.apiSecret)).execute()
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