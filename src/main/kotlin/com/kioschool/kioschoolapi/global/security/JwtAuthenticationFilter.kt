package com.kioschool.kioschoolapi.global.security

import com.kioschool.kioschoolapi.global.error.exception.CustomException
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpMethod.OPTIONS
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.servlet.HandlerExceptionResolver

class JwtAuthenticationFilter(
    private val allowedOrigin: String,
    private val jwtProvider: JwtProvider,
    private val handlerExceptionResolver: HandlerExceptionResolver,
) : OncePerRequestFilter() {

    // public (widened from protected) so unit tests can invoke it directly.
    public override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        try {
            val token = jwtProvider.resolveToken(request)
            if (!token.isNullOrBlank() && jwtProvider.isValidToken(token)) {
                val authentication = jwtProvider.getAuthentication(token)
                SecurityContextHolder.getContext().authentication = authentication
            }
        } catch (e: CustomException) {
            SecurityContextHolder.clearContext()
            handlerExceptionResolver.resolveException(request, response, null, e)
                ?: throw e
            return
        }

        filterChain.doFilter(request, response)
    }

    private fun HttpServletRequest.isPreflight(): Boolean {
        return method == OPTIONS.name()
    }

    private fun allowCors(response: HttpServletResponse) {
        response.setHeader(
            HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN,
            allowedOrigin
        )
        response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, true.toString())
        response.setHeader(
            HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS,
            "${HttpHeaders.CONTENT_TYPE}, ${HttpHeaders.AUTHORIZATION}"
        )
        response.setHeader(
            HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS,
            HttpMethod.values().joinToString { it.name() })
        response.status = HttpServletResponse.SC_OK
    }

    override fun shouldNotFilterAsyncDispatch(): Boolean {
        return false
    }
}