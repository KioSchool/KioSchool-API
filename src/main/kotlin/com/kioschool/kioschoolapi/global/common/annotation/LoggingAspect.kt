package com.kioschool.kioschoolapi.global.common.annotation

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Pointcut
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.util.StopWatch
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import org.springframework.web.multipart.MultipartFile
import java.util.*

@Aspect
@Component
class LoggingAspect(private val objectMapper: ObjectMapper) {

    private val log = LoggerFactory.getLogger(this.javaClass)

    // Pointcut: com.kioschool.kioschoolapi 하위의 모든 컨트롤러와 서비스에 적용
    @Pointcut(
        "within(@org.springframework.web.bind.annotation.RestController *)"
    )
    fun isApiService() {
    }

    @Around("isApiService()")
    fun logExecutionTime(joinPoint: ProceedingJoinPoint): Any? {
        val stopWatch = StopWatch()
        stopWatch.start()

        val className = joinPoint.signature.declaringTypeName
        val methodName = joinPoint.signature.name
        val args = joinPoint.args

        // Filter out web-related objects from arguments
        val filteredArgs = filterWebObjects(args)

        try {
            val request =
                (RequestContextHolder.currentRequestAttributes() as ServletRequestAttributes).request
            logRequestDetails(request)
        } catch (e: Exception) {
            log.warn("Could not log request details: {}", e.message)
        }

        // 메서드 실행 전 로그
        log.info(
            "--> {}#{}() called with args: {}",
            className,
            methodName,
            objectMapper.writeValueAsString(filteredArgs)
        )

        var result: Any? = null
        try {
            result = joinPoint.proceed()
            return result
        } catch (e: Exception) {
            log.error("<-- {}#{}() threw exception: {}", className, methodName, e.message)
            throw e // 예외를 다시 던져서 트랜잭션 등에 영향을 주지 않도록 함
        } finally {
            stopWatch.stop()
            val executionTime = stopWatch.totalTimeMillis

            // Filter out web-related objects from result
            val filteredResult = filterWebObjects(result)

            // 메서드 실행 후 로그
            log.info(
                "<-- {}#{}() returned: {} (execution time: {}ms)",
                className,
                methodName,
                objectMapper.writeValueAsString(filteredResult),
                executionTime
            )
        }
    }

    private fun logRequestDetails(request: HttpServletRequest) {
        val requestDetails = StringBuilder()
        requestDetails.append("\n--- [AOP] INCOMING REQUEST ---\n")
        requestDetails.append("URI    : ${request.requestURI}\n")
        requestDetails.append("Method : ${request.method}\n")
        requestDetails.append("Headers:\n")

        val headerNames = request.headerNames ?: Collections.emptyEnumeration()
        headerNames.asSequence().forEach { headerName ->
            requestDetails.append("  $headerName : ${request.getHeader(headerName)}\n")
        }
        requestDetails.append("----------------------------\n")
        log.info(requestDetails.toString())
    }

    private fun filterWebObjects(obj: Any?): Any? {
        return when (obj) {
            is Array<*> -> obj.map { filterWebObjects(it) }.toTypedArray()
            is Collection<*> -> obj.map { filterWebObjects(it) }
            is ServletRequest, is ServletResponse, is MultipartFile -> "[WEB_OBJECT]"
            else -> obj
        }
    }
}