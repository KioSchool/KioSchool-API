package com.kioschool.kioschoolapi.order.repository

import com.kioschool.kioschoolapi.order.util.OrderUtil
import org.springframework.data.redis.core.ValueOperations
import org.springframework.stereotype.Repository

@Repository
class OrderRedisRepository(
    private val valueOperations: ValueOperations<String, Int>
) {
    fun incrementOrderNumber(workspaceId: Long): Long {
        val key = OrderUtil.getOrderNumberKey(workspaceId)
        val orderNumber = valueOperations.increment(key, 1)

        return orderNumber ?: 1
    }

    fun resetOrderNumber(workspaceId: Long) {
        val key = OrderUtil.getOrderNumberKey(workspaceId)
        valueOperations.set(key, 1)
    }
}