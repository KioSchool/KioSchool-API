package com.kioschool.kioschoolapi.domain.order.repository

import com.kioschool.kioschoolapi.domain.order.util.OrderUtil
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.ValueOperations
import org.springframework.stereotype.Repository

@Repository
class OrderRedisRepository(
    private val valueOperations: ValueOperations<String, Int>,
    private val redisTemplate: RedisTemplate<String, Int>
) {
    fun incrementOrderNumber(workspaceId: Long): Long {
        val key = OrderUtil.Companion.getOrderNumberKey(workspaceId)
        val orderNumber = valueOperations.increment(key, 1)

        return orderNumber ?: 1
    }

    fun resetOrderNumber(workspaceId: Long) {
        val key = OrderUtil.Companion.getOrderNumberKey(workspaceId)
        redisTemplate.delete(key)
    }

    fun resetAllOrderNumber() {
        val keyPattern = OrderUtil.Companion.getAllOrderNumberKeyPattern()
        val keys = redisTemplate.keys(keyPattern)
        keys.forEach { key ->
            redisTemplate.delete(key)
        }
    }
}