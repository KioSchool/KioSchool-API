package com.kioschool.kioschoolapi.global.configuration

import io.micrometer.core.instrument.config.MeterFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class MetricsConfiguration {

    private val allowedPrefixes = listOf(
        "http.server.requests",
        "jvm.memory",
        "jvm.gc",
        "system.cpu",
        "hikaricp.connections"
    )

    @Bean
    fun uriTagFilter(): MeterFilter = MeterFilter.ignoreTags("uri")

    @Bean
    fun metricsWhitelistFilter(): MeterFilter = MeterFilter.deny { id ->
        allowedPrefixes.none { id.name.startsWith(it) }
    }
}
