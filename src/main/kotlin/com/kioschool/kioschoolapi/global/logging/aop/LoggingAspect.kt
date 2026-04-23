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
import org.springframework.stereotype.Component
import org.springframework.util.StopWatch
import org.springframework.web.multipart.MultipartFile
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
        val stopWatch = StopWatch().apply { start() }
        val className = joinPoint.signature.declaringTypeName
        val methodName = joinPoint.signature.name
        val filteredArgs = filterWebObjects(joinPoint.args)

        log.info(
            "--> {}#{}() called with args: {}",
            className,
            methodName,
            safeSerialize(filteredArgs)
        )

        return try {
            val result = joinPoint.proceed()
            stopWatch.stop()
            log.info(
                "<-- {}#{}() returned: {} ({}ms)",
                className,
                methodName,
                safeSerialize(filterWebObjects(result)),
                stopWatch.totalTimeMillis
            )
            result
        } catch (e: Exception) {
            log.error("<-- {}#{}() threw exception: {}", className, methodName, e.message)
            throw e
        }
    }

    // suspend 함수용 처리 (Mono를 이용해 비동기 완료 시점 캡처)
    private fun proceedSuspend(joinPoint: ProceedingJoinPoint): Any? {
        StopWatch().apply { start() }
        val className = joinPoint.signature.declaringTypeName
        val methodName = joinPoint.signature.name

        // Continuation을 제외한 인자만 로그에 남김
        val filteredArgs =
            filterWebObjects(joinPoint.args.take(joinPoint.args.size - 1).toTypedArray())
        log.info(
            "--> [SUSPEND] {}#{}() called with args: {}",
            className,
            methodName,
            safeSerialize(filteredArgs)
        )

        // Spring MVC에서 suspend 함수는 내부적으로 Mono/Flux 기반으로 동작하므로 proceed()를 그대로 반환
        // 단, 로그를 위해 실제 결과를 기다리고 싶다면 Webflux의 Mono로 브릿징이 필요합니다.
        // 현재 MVC의 suspend 컨트롤러는 Spring이 알아서 결과를 unwrap하므로,
        // AOP 수준에서 완벽한 '결과값 로그'를 찍으려면 아래와 같이 리턴값을 그대로 넘겨줘야 합니다.
        return try {
            val result = joinPoint.proceed()
            // 주의: 여기서 result는 COROUTINE_SUSPENDED일 확률이 높음
            // 따라서 finally에서 로그를 찍으면 '작업 완료' 시점이 아님을 인지해야 합니다.
            result
        } catch (e: Exception) {
            log.error("<-- [SUSPEND] {}#{}() threw exception: {}", className, methodName, e.message)
            throw e
        }
    }

    private fun safeSerialize(obj: Any?): String {
        return try {
            objectMapper.writeValueAsString(obj)
        } catch (e: Exception) {
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