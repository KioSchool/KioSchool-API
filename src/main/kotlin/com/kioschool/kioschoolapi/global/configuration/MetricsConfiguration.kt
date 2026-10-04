package com.kioschool.kioschoolapi.global.configuration

import io.micrometer.core.instrument.Meter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.config.MeterFilter
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import io.micrometer.stackdriver.StackdriverMeterRegistry
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration

@Configuration
class MetricsConfiguration {

    // Cloud Monitoring은 시계열 수에 비용이 붙어 필요한 메트릭만 uri 태그 없이 보낸다
    private val stackdriverAllowedPrefixes = listOf(
        "http.server.requests",
        "jvm.gc",
        "hikaricp.connections"
    )

    // 평소 p50 7ms·p99 20ms 안팎이라 낮은 구간을 촘촘하게 둔다
    private val httpServerRequestsBuckets = listOf(5L, 10, 15, 20, 30, 50, 100, 200, 500, 1000, 3000)
        .map { Duration.ofMillis(it).toNanos().toDouble() }
        .toDoubleArray()

    // MeterFilter 빈은 모든 레지스트리에 걸리므로, 레지스트리마다 다른 필터는 여기서 건다
    @Bean
    fun registryMeterFilters() = MeterRegistryCustomizer<MeterRegistry> { registry ->
        when (registry) {
            is StackdriverMeterRegistry -> registry.config()
                .meterFilter(MeterFilter.ignoreTags("uri"))
                .meterFilter(MeterFilter.deny { id -> stackdriverAllowedPrefixes.none { id.name.startsWith(it) } })

            is PrometheusMeterRegistry -> registry.config()
                .meterFilter(httpServerRequestsHistogram())
        }
    }

    private fun httpServerRequestsHistogram() = object : MeterFilter {
        override fun configure(id: Meter.Id, config: DistributionStatisticConfig): DistributionStatisticConfig {
            if (id.name != "http.server.requests") return config

            return DistributionStatisticConfig.builder()
                .serviceLevelObjectives(*httpServerRequestsBuckets)
                .build()
                .merge(config)
        }
    }
}
