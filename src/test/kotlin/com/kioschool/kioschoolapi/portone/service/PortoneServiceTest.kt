package com.kioschool.kioschoolapi.portone.service

import com.kioschool.kioschoolapi.global.error.ErrorCode
import com.kioschool.kioschoolapi.global.error.exception.CustomException
import com.kioschool.kioschoolapi.global.portone.api.PortoneApi
import com.kioschool.kioschoolapi.global.portone.dto.BaseResponse
import com.kioschool.kioschoolapi.global.portone.dto.GetAccountHolderResponse
import com.kioschool.kioschoolapi.global.portone.dto.GetTokenResponse
import com.kioschool.kioschoolapi.global.portone.service.PortoneService
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.assertThrows
import retrofit2.Call
import retrofit2.Response

class PortoneServiceTest : DescribeSpec({
    val portoneApi = mockk<PortoneApi>()
    val sut = PortoneService("api-key", "api-secret", portoneApi)

    afterTest {
        clearAllMocks()
    }

    fun stubToken() {
        val tokenCall = mockk<Call<BaseResponse<GetTokenResponse>>>()
        every { portoneApi.getToken(any()) } returns tokenCall
        every { tokenCall.execute() } returns Response.success(
            BaseResponse(0, null, GetTokenResponse("access-token", 0L, 0L))
        )
    }

    fun stubHolder(body: BaseResponse<GetAccountHolderResponse>?) {
        val holderCall = mockk<Call<BaseResponse<GetAccountHolderResponse>>>()
        every { portoneApi.getAccountHolder(any(), any(), any()) } returns holderCall
        every { holderCall.execute() } returns Response.success<BaseResponse<GetAccountHolderResponse>>(body)
    }

    describe("getAccountHolder") {
        it("조회에 성공하면 은행 등록 예금주명을 그대로 반환한다") {
            stubToken()
            stubHolder(BaseResponse(0, null, GetAccountHolderResponse("박지인(모임통장)")))

            sut.getAccountHolder("004", "1234567890") shouldBe "박지인(모임통장)"
        }

        it("잘려서 내려온 이름도 가공하지 않고 그대로 반환한다") {
            stubToken()
            stubHolder(BaseResponse(0, null, GetAccountHolderResponse("홍길동(건국대건축공")))

            sut.getAccountHolder("004", "1234567890") shouldBe "홍길동(건국대건축공"
        }

        it("앞뒤 공백은 제거한다") {
            stubToken()
            stubHolder(BaseResponse(0, null, GetAccountHolderResponse("  박지인  ")))

            sut.getAccountHolder("004", "1234567890") shouldBe "박지인"
        }

        it("body가 null이면 ACCOUNT_HOLDER_NOT_FOUND를 던진다") {
            stubToken()
            stubHolder(null)

            val ex = assertThrows<CustomException> { sut.getAccountHolder("004", "1234567890") }
            ex.errorCode shouldBe ErrorCode.ACCOUNT_HOLDER_NOT_FOUND
        }

        it("code가 0이 아니면 ACCOUNT_HOLDER_NOT_FOUND를 던진다") {
            stubToken()
            stubHolder(BaseResponse(-1, "존재하지 않는 계좌입니다.", null))

            val ex = assertThrows<CustomException> { sut.getAccountHolder("004", "1234567890") }
            ex.errorCode shouldBe ErrorCode.ACCOUNT_HOLDER_NOT_FOUND
        }

        it("bank_holder가 blank이면 ACCOUNT_HOLDER_NOT_FOUND를 던진다") {
            stubToken()
            stubHolder(BaseResponse(0, null, GetAccountHolderResponse("   ")))

            val ex = assertThrows<CustomException> { sut.getAccountHolder("004", "1234567890") }
            ex.errorCode shouldBe ErrorCode.ACCOUNT_HOLDER_NOT_FOUND
        }
    }
})
