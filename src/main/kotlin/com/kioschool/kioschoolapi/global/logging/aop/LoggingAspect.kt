package com.kioschool.kioschoolapi.global.logging.aop

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Pointcut
import org.aspectj.lang.reflect.MethodSignature
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.stereotype.Component
import org.springframework.util.StopWatch
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import org.springframework.web.multipart.MultipartFile
import java.util.UUID
import kotlin.coroutines.Continuation

@Aspect
@Component
class LoggingAspect(private val objectMapper: ObjectMapper) {

    private val log = LoggerFactory.getLogger(this.javaClass)

    @Pointcut("within(@org.springframework.web.bind.annotation.RestController *)")
    fun isApiService() {
    }

    @Around("isApiService()")
    fun logExecutionTime(joinPoint: ProceedingJoinPoint): Any? {
        val signature = joinPoint.signature as MethodSignature
        signature.method
        val args = joinPoint.args

        // 1. 마지막 인자가 Continuation인지 확인 (suspend 함수 여부 체크)
        val isSuspend = args.isNotEmpty() && args.last() is Continuation<*>

        return if (isSuspend) {
            // suspend 함수인 경우 전용 처리 로직 호출
            proceedSuspend(joinPoint)
        } else {
            // 일반 함수인 경우 기존 로직 수행
            proceedNormal(joinPoint)
        }
    }

    // 일반 함수용 처리
    private fun proceedNormal(joinPoint: ProceedingJoinPoint): Any? {
        val requestId = UUID.randomUUID().toString().substring(0, 8)
        MDC.put("requestId", requestId)

        val stopWatch = StopWatch().apply { start() }
        val className = joinPoint.signature.declaringType.simpleName
        val methodName = joinPoint.signature.name
        val filteredArgs = filterWebObjects(joinPoint.args)

        log.info(
            "--> [{}] {}#{}() called with args: {}",
            requestId,
            className,
            methodName,
            safeSerialize(filteredArgs)
        )

        return try {
            val result = joinPoint.proceed()
            stopWatch.stop()
            log.info(
                "<-- [{}] {}#{}() returned: {} ({}ms)",
                requestId, className, methodName,
                safeSerialize(filterWebObjects(result)),
                stopWatch.totalTimeMillis
            )
            result
        } catch (e: Exception) {
            val httpInfo = resolveHttpInfo()
            log.error(
                "<-- [{}] {}#{}() threw {}: {} | {} {}\n{}",
                requestId, className, methodName,
                e.javaClass.simpleName, e.message,
                httpInfo.first, httpInfo.second,
                formatStackTrace(e)
            )
            throw e
        } finally {
            MDC.remove("requestId")
        }
    }

    // suspend 함수용 처리
    private fun proceedSuspend(joinPoint: ProceedingJoinPoint): Any? {
        val requestId = UUID.randomUUID().toString().substring(0, 8)
        MDC.put("requestId", requestId)

        val className = joinPoint.signature.declaringType.simpleName
        val methodName = joinPoint.signature.name
        val filteredArgs =
            filterWebObjects(joinPoint.args.take(joinPoint.args.size - 1).toTypedArray())

        log.info(
            "--> [SUSPEND] [{}] {}#{}() called with args: {}",
            requestId,
            className,
            methodName,
            safeSerialize(filteredArgs)
        )

        return try {
            joinPoint.proceed()
        } catch (e: Exception) {
            val httpInfo = resolveHttpInfo()
            log.error(
                "<-- [SUSPEND] [{}] {}#{}() threw {}: {} | {} {}\n{}",
                requestId, className, methodName,
                e.javaClass.simpleName, e.message,
                httpInfo.first, httpInfo.second,
                formatStackTrace(e)
            )
            throw e
        } finally {
            MDC.remove("requestId")
        }
    }

    private fun resolveHttpInfo(): Pair<String, String> {
        val request = (RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes)?.request
        return Pair(request?.method ?: "UNKNOWN", request?.requestURI ?: "UNKNOWN")
    }

    private fun formatStackTrace(e: Exception, maxLines: Int = 10): String {
        val lines = mutableListOf<String>()
        e.stackTrace.take(maxLines).forEach { lines.add("\tat $it") }
        if (e.stackTrace.size > maxLines) lines.add("\t... ${e.stackTrace.size - maxLines} more")
        e.cause?.let { cause ->
            lines.add("Caused by: $cause")
            cause.stackTrace.take(3).forEach { lines.add("\tat $it") }
        }
        return lines.joinToString("\n")
    }

    private fun safeSerialize(obj: Any?): String {
        return try {
            objectMapper.writeValueAsString(obj)
        } catch (e: Exception) {
            log.warn("Failed to serialize object: {}", e.message)
            "[SERIALIZATION_ERROR]"
        }
    }

    private fun filterWebObjects(obj: Any?): Any? {
        return when (obj) {
            is Array<*> -> obj.filter { it !is Continuation<*> }.map { filterWebObjects(it) }
                .toTypedArray()

            is Collection<*> -> obj.filter { it !is Continuation<*> }.map { filterWebObjects(it) }
            is ServletRequest, is ServletResponse, is MultipartFile, is Continuation<*> -> "[NON_SERIALIZABLE]"
            else -> obj
        }
    }
}