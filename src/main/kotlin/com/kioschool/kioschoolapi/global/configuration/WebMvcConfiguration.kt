package com.kioschool.kioschoolapi.global.configuration

import com.kioschool.kioschoolapi.global.common.resolver.AuthenticationArgumentResolver
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebMvcConfiguration(
    @Value("\${websocket.allowed-origins}")
    private val allowedOrigins: String,
    private val authenticationArgumentResolver: AuthenticationArgumentResolver,
) : WebMvcConfigurer {
    override fun addCorsMappings(registry: CorsRegistry) {
        registry
            .addMapping("/**")
            .allowedHeaders("*")
            .allowedOrigins(allowedOrigins)
            .allowedMethods("*")
            .allowCredentials(true)
    }

    override fun addArgumentResolvers(resolvers: MutableList<HandlerMethodArgumentResolver>) {
        super.addArgumentResolvers(resolvers)
        resolvers.add(authenticationArgumentResolver)
    }
}