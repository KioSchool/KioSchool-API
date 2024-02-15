package com.kioschool.kioschoolapi.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter

class JwtAuthenticationFilter(
    private val jwtProvider: JwtProvider
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val token = jwtProvider.resolveToken(request)

        if (request.method == "OPTIONS") {
            response.setHeader(
                "Access-Control-Allow-Origin",
                request.getHeader("Origin")
            )
            response.setHeader("Access-Control-Allow-Credentials", "true")
            response.setHeader("Access-Control-Allow-Headers", "content-type, Authorization")
            response.setHeader("Access-Control-Allow-Methods", "POST, GET, PUT, DELETE, OPTIONS")
            response.status = HttpServletResponse.SC_OK
            return
        }

        if (token != null && jwtProvider.validateToken(token)) {
            val authentication = jwtProvider.getAuthentication(token)
            SecurityContextHolder.getContext().authentication = authentication
        }

        filterChain.doFilter(request, response)
    }
}