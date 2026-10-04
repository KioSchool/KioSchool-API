package com.kioschool.kioschoolapi.global.aspect

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.micrometer.core.instrument.observation.DefaultMeterObservationHandler
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.micrometer.observation.ObservationRegistry
import io.mockk.every
import io.mockk.mockk
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.Signature

class GlobalObservationAspectTest : DescribeSpec({
    class SampleFacade

    fun joinPoint(methodName: String, result: () -> Any?): ProceedingJoinPoint {
        val signature = mockk<Signature> {
            every { declaringType } returns SampleFacade::class.java
            every { name } returns methodName
        }
        return mockk {
            every { this@mockk.signature } returns signature
            every { proceed() } answers { result() }
        }
    }

    describe("GlobalObservationAspect") {
        val meterRegistry = SimpleMeterRegistry()
        val observationRegistry = ObservationRegistry.create().apply {
            observationConfig().observationHandler(DefaultMeterObservationHandler(meterRegistry))
        }
        val sut = GlobalObservationAspect(observationRegistry)

        // 클래스 이름이 메트릭 이름이 되면 Prometheus에서 메서드별로 묶어 볼 수 없다 — 한 이름에 클래스·메서드를 태그로 단다
        it("records each method under one meter name tagged with class and method") {
            sut.observeAllDomainMethods(joinPoint("createOrder") { "ok" }) shouldBe "ok"
            sut.observeAllDomainMethods(joinPoint("createOrder") { "ok" })

            meterRegistry.get("kioschool.domain")
                .tags("class", "SampleFacade", "method", "createOrder")
                .timer().count() shouldBe 2
        }

        it("still records and rethrows when the method fails") {
            shouldThrow<IllegalStateException> {
                sut.observeAllDomainMethods(joinPoint("failing") { throw IllegalStateException("boom") })
            }

            meterRegistry.get("kioschool.domain")
                .tags("class", "SampleFacade", "method", "failing", "error", "IllegalStateException")
                .timer().count() shouldBe 1
        }
    }
})
