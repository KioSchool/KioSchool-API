package com.kioschool.kioschoolapi.portone.api

import com.kioschool.kioschoolapi.portone.dto.BaseResponse
import com.kioschool.kioschoolapi.portone.dto.GetAccountHolderResponse
import com.kioschool.kioschoolapi.portone.dto.GetTokenRequest
import com.kioschool.kioschoolapi.portone.dto.GetTokenResponse
import retrofit2.Call
import retrofit2.http.*

interface PortoneApi {
    @POST("/users/getToken")
    fun getToken(
        @Body body: GetTokenRequest
    ): Call<BaseResponse<GetTokenResponse>>

    @GET("/vbanks/holder")
    fun getAccountHolder(
        @Header("Authorization") accessToken: String,
        @Query("bank_code") bank: String,
        @Query("bank_num") accountNumber: String
    ): Call<BaseResponse<GetAccountHolderResponse>>
}