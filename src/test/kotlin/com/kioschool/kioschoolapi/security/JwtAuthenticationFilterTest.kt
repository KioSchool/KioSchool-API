package com.kioschool.kioschoolapi.security

import com.kioschool.kioschoolapi.global.error.ErrorCode
import com.kioschool.kioschoolapi.global.error.exception.CustomException
import com.kioschool.kioschoolapi.global.security.JwtAuthenticationFilter
import com.kioschool.kioschoolapi.global.security.JwtProvider
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.servlet.HandlerExceptionResolver

class JwtAuthenticationFilterTest : DescribeSpec({
    afterTest { SecurityContextHolder.clearContext() }

    describe("doFilterInternal") {
        it("인증 중 CustomException이 나면 resolver로 위임하고 체인을 진행하지 않는다") {
            val jwtProvider = mockk<JwtProvider>()
            val resolver = mockk<HandlerExceptionResolver>(relaxed = true)
            val filter = JwtAuthenticationFilter("*", jwtProvider, resolver)

            val request = mockk<HttpServletRequest>(relaxed = true)
            val response = mockk<HttpServletResponse>(relaxed = true)
            val chain = mockk<FilterChain>(relaxed = true)

            every { jwtProvider.resolveToken(request) } returns "token"
            every { jwtProvider.isValidToken("token") } returns true
            every { jwtProvider.getAuthentication("token") } throws CustomException(ErrorCode.INVALID_JWT)

            filter.doFilterInternal(request, response, chain)

            verify(exactly = 1) { resolver.resolveException(request, response, null, any()) }
            verify(exactly = 0) { chain.doFilter(any(), any()) }
        }

        it("토큰이 없으면 인증 없이 체인을 진행한다") {
            val jwtProvider = mockk<JwtProvider>()
            val resolver = mockk<HandlerExceptionResolver>(relaxed = true)
            val filter = JwtAuthenticationFilter("*", jwtProvider, resolver)

            val request = mockk<HttpServletRequest>(relaxed = true)
            val response = mockk<HttpServletResponse>(relaxed = true)
            val chain = mockk<FilterChain>(relaxed = true)

            every { jwtProvider.resolveToken(request) } returns null

            filter.doFilterInternal(request, response, chain)

            verify(exactly = 1) { chain.doFilter(request, response) }
            verify(exactly = 0) { resolver.resolveException(any(), any(), any(), any()) }
        }

        it("유효한 토큰이면 인증을 설정하고 체인을 진행한다") {
            val jwtProvider = mockk<JwtProvider>()
            val resolver = mockk<HandlerExceptionResolver>(relaxed = true)
            val filter = JwtAuthenticationFilter("*", jwtProvider, resolver)
            val request = mockk<HttpServletRequest>(relaxed = true)
            val response = mockk<HttpServletResponse>(relaxed = true)
            val chain = mockk<FilterChain>(relaxed = true)
            val auth = UsernamePasswordAuthenticationToken("user", "", emptyList())

            every { jwtProvider.resolveToken(request) } returns "token"
            every { jwtProvider.isValidToken("token") } returns true
            every { jwtProvider.getAuthentication("token") } returns auth

            filter.doFilterInternal(request, response, chain)

            SecurityContextHolder.getContext().authentication shouldBe auth
            verify(exactly = 1) { chain.doFilter(request, response) }
        }

        it("CustomException이 아닌 예외는 필터에서 잡지 않고 전파된다") {
            val jwtProvider = mockk<JwtProvider>()
            val resolver = mockk<HandlerExceptionResolver>(relaxed = true)
            val filter = JwtAuthenticationFilter("*", jwtProvider, resolver)
            val request = mockk<HttpServletRequest>(relaxed = true)
            val response = mockk<HttpServletResponse>(relaxed = true)
            val chain = mockk<FilterChain>(relaxed = true)

            every { jwtProvider.resolveToken(request) } returns "token"
            every { jwtProvider.isValidToken("token") } returns true
            every { jwtProvider.getAuthentication("token") } throws RuntimeException("boom")

            shouldThrow<RuntimeException> {
                filter.doFilterInternal(request, response, chain)
            }
            verify(exactly = 0) { chain.doFilter(any(), any()) }
        }
    }
})
