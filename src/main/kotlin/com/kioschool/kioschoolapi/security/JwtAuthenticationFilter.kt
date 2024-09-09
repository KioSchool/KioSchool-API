package com.kioschool.kioschoolapi.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter

class JwtAuthenticationFilter(
    private val allowedOrigin: String,
    private val jwtProvider: JwtProvider
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        if (request.isPreflight()) {
            allowCors(request, response)
            return
        }

        val token = jwtProvider.resolveToken(request)
        if (token != null && jwtProvider.isValidToken(token)) {
            val authentication = jwtProvider.getAuthentication(token)
            SecurityContextHolder.getContext().authentication = authentication
        }

        filterChain.doFilter(request, response)
    }

    private fun HttpServletRequest.isPreflight(): Boolean {
        return method == "OPTIONS"
    }

    private fun allowCors(request: HttpServletRequest, response: HttpServletResponse) {
        response.setHeader(
            "Access-Control-Allow-Origin",
            allowedOrigin
        )
        response.setHeader("Access-Control-Allow-Credentials", "true")
        response.setHeader("Access-Control-Allow-Headers", "content-type, Authorization")
        response.setHeader("Access-Control-Allow-Methods", "POST, GET, PUT, DELETE, OPTIONS")
        response.status = HttpServletResponse.SC_OK
    }
}