package com.kioschool.kioschoolapi.global.cache.listener

import com.kioschool.kioschoolapi.domain.product.event.ProductUpdatedEvent
import com.kioschool.kioschoolapi.domain.workspace.event.WorkspaceUpdatedEvent
import org.springframework.cache.CacheManager
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class WorkspaceCacheEvictListener(
    private val cacheManager: CacheManager
) {

    @EventListener
    fun handleProductUpdate(event: ProductUpdatedEvent) {
        cacheManager.getCache("workspaces")?.evict(event.workspaceId)
    }

    @EventListener
    fun handleWorkspaceUpdate(event: WorkspaceUpdatedEvent) {
        cacheManager.getCache("workspaces")?.evict(event.workspaceId)
    }
}
