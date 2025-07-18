package com.kioschool.kioschoolapi.global.configuration

import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class QueryDslConfiguration(
    @PersistenceContext
    private val entityManager: EntityManager
) {
    @Bean
    fun queryFactory() = JPAQueryFactory(entityManager)
}