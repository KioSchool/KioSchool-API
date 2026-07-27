package com.kioschool.kioschoolapi.global.security.handler

import com.fasterxml.jackson.databind.ObjectMapper
import com.kioschool.kioschoolapi.global.error.ErrorCode
import com.kioschool.kioschoolapi.global.error.dto.ErrorResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.MediaType
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.stereotype.Component

/**
 * Security 계층에서 거부되는 요청은 DispatcherServlet 이전에 처리되어
 * [org.springframework.web.bind.annotation.RestControllerAdvice]를 우회한다.
 * 따라서 여기서 GlobalExceptionHandler와 동일한 [ErrorResponse] JSON을 직접 써서
 * 인증/인가 실패 응답에도 안정적인 `code`가 실리도록 한다.
 */
internal fun writeErrorResponse(
    request: HttpServletRequest,
    response: HttpServletResponse,
    objectMapper: ObjectMapper,
    errorCode: ErrorCode,
) {
    response.status = errorCode.status.value()
    response.contentType = MediaType.APPLICATION_JSON_VALUE
    response.characterEncoding = Charsets.UTF_8.name()
    val body = ErrorResponse.of(errorCode, request.requestURI)
    response.writer.write(objectMapper.writeValueAsString(body))
}

/**
 * 인증 정보가 없거나 만료되어 익명 사용자로 거부될 때(→ 401).
 * 쿠키 부재/만료 토큰은 JwtAuthenticationFilter가 예외를 던지지 않고 통과시키므로
 * 이 EntryPoint가 세션 만료 응답에 `AUTHENTICATION_REQUIRED` code를 실어준다.
 */
@Component
class CustomAuthenticationEntryPoint(
    private val objectMapper: ObjectMapper,
) : AuthenticationEntryPoint {
    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authException: AuthenticationException,
    ) {
        writeErrorResponse(request, response, objectMapper, ErrorCode.AUTHENTICATION_REQUIRED)
    }
}

/**
 * 인증은 됐으나 권한(role)이 부족할 때(→ 403). 세션 자체는 유효하다.
 */
@Component
class CustomAccessDeniedHandler(
    private val objectMapper: ObjectMapper,
) : AccessDeniedHandler {
    override fun handle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        accessDeniedException: AccessDeniedException,
    ) {
        writeErrorResponse(request, response, objectMapper, ErrorCode.ACCESS_DENIED)
    }
}
