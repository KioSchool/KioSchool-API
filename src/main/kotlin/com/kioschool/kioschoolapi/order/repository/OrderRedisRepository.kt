package com.kioschool.kioschoolapi.order.repository

import org.springframework.data.redis.core.ValueOperations
import org.springframework.stereotype.Repository

@Repository
class OrderRedisRepository(
    private val valueOperations: ValueOperations<String, Int>
) {
    fun incrementOrderNumber(workspaceId: Long): Long {
        val key = "order_number_$workspaceId"
        val orderNumber = valueOperations.increment(key, 1)

        return orderNumber ?: 1
    }
}