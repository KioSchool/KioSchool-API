package com.kioschool.kioschoolapi.global.cache.aop

import com.kioschool.kioschoolapi.domain.workspace.entity.Workspace
import com.kioschool.kioschoolapi.domain.workspace.event.WorkspaceUpdatedEvent
import org.aspectj.lang.annotation.AfterReturning
import org.aspectj.lang.annotation.Aspect
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component

@Aspect
@Component
class WorkspaceEventAspect(
    private val eventPublisher: ApplicationEventPublisher
) {

    @AfterReturning(
        pointcut = "@annotation(com.kioschool.kioschoolapi.global.cache.annotation.WorkspaceUpdateEvent)",
        returning = "workspace"
    )
    fun handleWorkspaceUpdate(workspace: Workspace) {
        eventPublisher.publishEvent(WorkspaceUpdatedEvent(workspace.id))
    }
}
