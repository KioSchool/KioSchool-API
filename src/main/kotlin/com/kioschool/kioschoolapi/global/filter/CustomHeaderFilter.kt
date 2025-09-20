package com.kioschool.kioschoolapi.global.filter

import jakarta.servlet.Filter
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component

@Component
class CustomHeaderFilter : Filter {
    override fun doFilter(
        request: ServletRequest,
        response: ServletResponse,
        chain: FilterChain
    ) {
        val httpServletResponse = response as HttpServletResponse


        httpServletResponse.setHeader("Document-Policy", " js-profiling")


        chain.doFilter(request, response)
    }
}