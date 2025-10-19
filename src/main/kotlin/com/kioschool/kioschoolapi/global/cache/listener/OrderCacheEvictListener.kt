package com.kioschool.kioschoolapi.global.cache.listener

import com.kioschool.kioschoolapi.domain.order.event.OrderUpdatedEvent
import com.kioschool.kioschoolapi.global.cache.constant.CacheNames
import org.springframework.cache.CacheManager
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class OrderCacheEvictListener(
    private val cacheManager: CacheManager
) {

    @EventListener
    fun handleOrderUpdate(event: OrderUpdatedEvent) {
        cacheManager.getCache(CacheNames.ORDERS)?.evict(event.orderId)
    }
}
