package com.kioschool.kioschoolapi.global.configuration

import org.springdoc.core.models.GroupedOpenApi
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
@Profile("!prod")
class SwaggerConfiguration {
    @Bean
    fun superAdminGroup(): GroupedOpenApi {
        return GroupedOpenApi.builder()
            .group("super-admin")
            .pathsToMatch("/super-admin/**")
            .build()
    }

    @Bean
    fun adminGroup(): GroupedOpenApi {
        return GroupedOpenApi.builder()
            .group("admin")
            .pathsToMatch("/admin/**")
            .build()
    }

    @Bean
    fun userGroup(): GroupedOpenApi {
        return GroupedOpenApi.builder()
            .group("user")
            .pathsToExclude("/admin/**", "/super-admin/**")
            .build()
    }
}