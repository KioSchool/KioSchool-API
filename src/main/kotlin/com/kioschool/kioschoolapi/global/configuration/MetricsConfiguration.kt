package com.kioschool.kioschoolapi.global.configuration

import io.lettuce.core.metrics.MicrometerOptions
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

    // 평소 p50 7ms·p99 20ms 안팎이라 낮은 구간을 촘촘하게, 장애 때 보이는 100ms~10s 구간도 p95·p99가 뭉개지지 않게 나눈다
    private val httpServerRequestsBuckets =
        listOf(5L, 10, 15, 20, 30, 50, 75, 100, 150, 200, 300, 500, 750, 1000, 2000, 3000, 5000, 10000)
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

    // Redis 명령별 p95·p99를 보려고 히스토그램을 켠다 (기본은 평균·최대만). 명령 타임아웃이 3초라 상한은 5초면 충분
    @Bean
    fun lettuceMicrometerOptions(): MicrometerOptions = MicrometerOptions.builder()
        .histogram(true)
        .maxLatency(Duration.ofSeconds(5))
        .build()

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
