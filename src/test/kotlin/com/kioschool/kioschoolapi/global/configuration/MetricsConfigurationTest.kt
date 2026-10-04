package com.kioschool.kioschoolapi.global.configuration

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.micrometer.core.instrument.Clock
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import io.micrometer.stackdriver.StackdriverConfig
import io.micrometer.stackdriver.StackdriverMeterRegistry
import java.time.Duration

class MetricsConfigurationTest : DescribeSpec({
    val customizer = MetricsConfiguration().registryMeterFilters()

    fun recordRequest(registry: MeterRegistry) =
        Timer.builder("http.server.requests")
            .tag("uri", "/admin/workspace/tables")
            .tag("status", "200")
            .register(registry)
            .record(Duration.ofMillis(7))

    describe("Stackdriver registry") {
        // Cloud Monitoring은 시계열 수에 비용이 붙는다 — Prometheus를 붙여도 기존 화이트리스트·uri 제거가 유지돼야 한다
        val config = StackdriverConfig { key ->
            when (key) {
                "stackdriver.projectId" -> "test-project"
                "stackdriver.enabled" -> "false"
                else -> null
            }
        }
        val registry = StackdriverMeterRegistry(config, Clock.SYSTEM).also { customizer.customize(it) }

        it("keeps only whitelisted meters") {
            registry.gauge("executor.active", 1)

            registry.find("executor.active").meters().shouldBeEmpty()
        }

        it("drops the uri tag") {
            recordRequest(registry)

            registry.get("http.server.requests").timer().id.getTag("uri").shouldBeNull()
        }
    }

    describe("Prometheus registry") {
        val registry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT).also { customizer.customize(it) }

        it("keeps meters outside the Stackdriver whitelist") {
            registry.gauge("executor.active", 1)

            registry.find("executor.active").meters().size shouldBe 1
        }

        it("publishes http.server.requests per uri with histogram buckets") {
            recordRequest(registry)

            registry.scrape() shouldContain
                "http_server_requests_seconds_bucket{status=\"200\",uri=\"/admin/workspace/tables\",le=\"0.01\",} 1.0"
        }
    }
})
