package com.kioschool.kioschoolapi.global.cache.listener

import com.kioschool.kioschoolapi.domain.product.event.ProductUpdatedEvent
import com.kioschool.kioschoolapi.domain.workspace.event.WorkspaceUpdatedEvent
import com.kioschool.kioschoolapi.global.cache.constant.CacheNames
import org.springframework.cache.CacheManager
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class WorkspaceCacheEvictListener(
    private val cacheManager: CacheManager
) {

    @EventListener
    fun handleProductUpdate(event: ProductUpdatedEvent) {
        cacheManager.getCache(CacheNames.WORKSPACES)?.evict(event.workspaceId)
    }

    @EventListener
    fun handleWorkspaceUpdate(event: WorkspaceUpdatedEvent) {
        cacheManager.getCache(CacheNames.WORKSPACES)?.evict(event.workspaceId)
    }
}
