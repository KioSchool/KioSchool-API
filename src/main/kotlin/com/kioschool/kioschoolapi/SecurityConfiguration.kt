package com.kioschool.kioschoolapi

import com.kioschool.kioschoolapi.common.enums.UserRole
import com.kioschool.kioschoolapi.security.JwtAuthenticationFilter
import com.kioschool.kioschoolapi.security.JwtProvider
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.web.DefaultSecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
@EnableWebSecurity
class SecurityConfiguration(
    @Value("\${websocket.allowed-origins}")
    private val allowedOrigins: String,
    private val jwtProvider: JwtProvider
) {

    @Bean
    fun filterChain(httpSecurity: HttpSecurity): DefaultSecurityFilterChain? {
        httpSecurity
            .csrf { it.disable() }
            .authorizeHttpRequests { it ->
                it.requestMatchers("/admin/**")
                    .hasAnyAuthority(UserRole.SUPER_ADMIN.name, UserRole.ADMIN.name)
            }
            .authorizeHttpRequests { it.requestMatchers("/**").permitAll() }
            .addFilterBefore(
                JwtAuthenticationFilter(allowedOrigins, jwtProvider),
                UsernamePasswordAuthenticationFilter::class.java
            ).logout {
                it.disable()
            }

        return httpSecurity.build()
    }

    @Bean("passwordEncoder")
    fun passwordEncoder() = BCryptPasswordEncoder()
}