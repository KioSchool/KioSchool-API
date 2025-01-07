package com.kioschool.kioschoolapi

import com.kioschool.kioschoolapi.common.resolver.AuthenticationArgumentResolver
import com.kioschool.kioschoolapi.common.resolver.SuperAdminAuthenticationArgumentResolver
import org.springframework.context.annotation.Configuration
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebMvcConfiguration(
    private val authenticationArgumentResolver: AuthenticationArgumentResolver,
    private val superAdminAuthenticationArgumentResolver: SuperAdminAuthenticationArgumentResolver
) : WebMvcConfigurer {
    override fun addCorsMappings(registry: CorsRegistry) {
        registry
            .addMapping("/**")
            .allowedHeaders("*")
            .allowedOrigins("http://localhost:3000", "https://kio-school.com")
            .allowedMethods("*")
            .allowCredentials(true)
    }

    override fun addArgumentResolvers(resolvers: MutableList<HandlerMethodArgumentResolver>) {
        super.addArgumentResolvers(resolvers)
        resolvers.add(authenticationArgumentResolver)
        resolvers.add(superAdminAuthenticationArgumentResolver)
    }
}