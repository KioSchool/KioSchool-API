package com.kioschool.kioschoolapi.bank.service

import com.kioschool.kioschoolapi.bank.repository.BankCodeRepository
import com.kioschool.kioschoolapi.common.service.ApiService
import okhttp3.FormBody
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.json.GsonJsonParser
import org.springframework.stereotype.Service
import java.net.URLDecoder

@Service
class BankService(
    private val bankCodeRepository: BankCodeRepository,
    @Value("\${portone.api-key}")
    private val apiKey: String,
    @Value("\${portone.api-secret}")
    private val apiSecret: String,
    private val apiService: ApiService
) {
    fun getBankAccountHolderName(name: String, accountNumber: String): String? {
        val accessToken = getAccessToken()
        val bankCode = getBankCodeCode(name)

        val url = "https://api.iamport.kr/vbanks/holder"
        val query = "bank_code=$bankCode&bank_num=$accountNumber"
        val headers = arrayOf("Authorization", accessToken)
        val response = apiService.get(url, query, headers)

        return parseBankAccountHolderName(response.body!!.string())
    }

    private fun getBankCodeCode(name: String): String? {
        val decodedName = URLDecoder.decode(name, "UTF-8")
        return bankCodeRepository.findByName(decodedName)?.code
    }

    private fun getAccessToken(): String {
        val url = "https://api.iamport.kr/users/getToken"
        val body = FormBody.Builder()
            .add("imp_key", apiKey)
            .add("imp_secret", apiSecret)
            .build()
        val response = apiService.post(url, body)

        return parseAccessToken(response.body!!.string())
    }

    private fun parseAccessToken(response: String): String {
        val jsonParser = GsonJsonParser()
        val map = jsonParser.parseMap(response)
        val responseMap = jsonParser.parseMap(map["response"].toString())
        return responseMap["access_token"].toString()
    }

    private fun parseBankAccountHolderName(response: String): String {
        val jsonParser = GsonJsonParser()
        val map = jsonParser.parseMap(response)
        val responseMap = jsonParser.parseMap(map["response"].toString())
        return responseMap["bank_holder"].toString()
    }
}