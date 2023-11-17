package com.kioschool.kioschoolapi

import com.kioschool.kioschoolapi.common.enums.UserRole
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.web.DefaultSecurityFilterChain

@Configuration
@EnableWebSecurity
class SecurityConfiguration {

    @Bean
    fun filterChain(httpSecurity: HttpSecurity): DefaultSecurityFilterChain? {
        httpSecurity
            .csrf { it.disable() }
            .authorizeHttpRequests {
                it.requestMatchers("/admin/**")
                    .hasAnyRole(listOf(UserRole.ADMIN, UserRole.SUPER_ADMIN).joinToString { "," })
            }
            .authorizeHttpRequests { it.requestMatchers("/**").permitAll() }

        return httpSecurity.build()
    }
}