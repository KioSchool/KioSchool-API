package com.kioschool.kioschoolapi.global.cache.listener

import com.kioschool.kioschoolapi.domain.product.event.ProductCategoryUpdatedEvent
import com.kioschool.kioschoolapi.global.cache.constant.CacheNames
import org.springframework.cache.CacheManager
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class ProductCategoryCacheEvictListener(
    private val cacheManager: CacheManager
) {

    @EventListener
    fun handleProductCategoryUpdate(event: ProductCategoryUpdatedEvent) {
        cacheManager.getCache(CacheNames.PRODUCT_CATEGORIES)?.evict(event.workspaceId)
    }
}
