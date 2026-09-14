package com.kioschool.kioschoolapi.global.turnstile.dto

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class SiteverifyResponseTest : DescribeSpec({
    describe("역직렬화") {
        it("Cloudflare 응답의 error-codes를 읽고 모르는 필드는 무시한다") {
            val json = """{"success":false,"error-codes":["invalid-input-response"],"messages":[],"hostname":"kio-school.com"}"""

            val response = jacksonObjectMapper().readValue<SiteverifyResponse>(json)

            response shouldBe SiteverifyResponse(success = false, errorCodes = listOf("invalid-input-response"))
        }
    }
})
