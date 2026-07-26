package com.kioschool.kioschoolapi.security

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.kioschool.kioschoolapi.global.security.handler.CustomAccessDeniedHandler
import com.kioschool.kioschoolapi.global.security.handler.CustomAuthenticationEntryPoint
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.AuthenticationException
import java.io.PrintWriter
import java.io.StringWriter

class SecurityErrorHandlersTest : DescribeSpec({
    val objectMapper = ObjectMapper().registerModule(JavaTimeModule())

    fun request(uri: String): HttpServletRequest = mockk { every { requestURI } returns uri }

    fun responseWithWriter(sink: StringWriter): HttpServletResponse =
        mockk(relaxed = true) { every { writer } returns PrintWriter(sink) }

    describe("CustomAuthenticationEntryPoint") {
        it("미인증 요청에 401 + code=AUTHENTICATION_REQUIRED JSON을 쓴다") {
            val sink = StringWriter()
            val response = responseWithWriter(sink)
            val sut = CustomAuthenticationEntryPoint(objectMapper)

            sut.commence(request("/admin/workspace"), response, mockk<AuthenticationException>())

            verify { response.status = 401 }
            verify { response.contentType = "application/json" }
            val body = sink.toString()
            body shouldContain "\"code\":\"AUTHENTICATION_REQUIRED\""
            body shouldContain "\"status\":401"
            body shouldContain "\"path\":\"/admin/workspace\""
        }
    }

    describe("CustomAccessDeniedHandler") {
        it("권한 부족 요청에 403 + code=ACCESS_DENIED JSON을 쓴다") {
            val sink = StringWriter()
            val response = responseWithWriter(sink)
            val sut = CustomAccessDeniedHandler(objectMapper)

            sut.handle(request("/super-admin/emails"), response, mockk<AccessDeniedException>())

            verify { response.status = 403 }
            val body = sink.toString()
            body shouldContain "\"code\":\"ACCESS_DENIED\""
            body shouldContain "\"status\":403"
        }
    }

    describe("ErrorCode 정합성") {
        it("두 인증 실패 코드의 status가 의도한 값이다") {
            com.kioschool.kioschoolapi.global.error.ErrorCode.AUTHENTICATION_REQUIRED.status.value() shouldBe 401
            com.kioschool.kioschoolapi.global.error.ErrorCode.ACCESS_DENIED.status.value() shouldBe 403
        }
    }
})
