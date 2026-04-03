package com.kioschool.kioschoolapi.global.aspect

import io.micrometer.observation.Observation
import io.micrometer.observation.ObservationRegistry
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.springframework.stereotype.Component

@Aspect
@Component
class GlobalObservationAspect(
    private val observationRegistry: ObservationRegistry
) {
    // domain 하위의 모든 Facade와 Service 내의 메서드를 전부 캡처합니다
    @Around("execution(* com.kioschool.kioschoolapi.domain..*Facade.*(..)) || execution(* com.kioschool.kioschoolapi.domain..*Service.*(..))")
    fun observeAllDomainMethods(joinPoint: ProceedingJoinPoint): Any? {
        val className = joinPoint.signature.declaringType.simpleName
        val methodName = joinPoint.signature.name
        
        val observation = Observation.createNotStarted(className, observationRegistry)
            .contextualName("$className.$methodName")
            .start()
            
        try {
            return observation.openScope().use { _ ->
                joinPoint.proceed()
            }
        } catch (ex: Throwable) {
            observation.error(ex)
            throw ex
        } finally {
            observation.stop()
        }
    }
}
