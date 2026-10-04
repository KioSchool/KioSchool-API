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
        
        // 이름을 하나로 두고 클래스·메서드는 태그로 단다 — Prometheus에서 메서드별 처리 시간을 묶어 보기 위해.
        // 트레이스 span 이름은 contextualName("클래스.메서드")이라 그대로다
        val observation = Observation.createNotStarted("kioschool.domain", observationRegistry)
            .contextualName("$className.$methodName")
            .lowCardinalityKeyValue("class", className)
            .lowCardinalityKeyValue("method", methodName)
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
