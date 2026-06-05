package com.kioschool.kioschoolapi.domain.order.controller

import com.kioschool.kioschoolapi.domain.workspace.exception.WorkspaceInaccessibleException
import com.kioschool.kioschoolapi.domain.workspace.service.WorkspaceService
import com.kioschool.kioschoolapi.global.security.annotation.AdminUsername
import com.kioschool.kioschoolapi.global.sse.SseEmitterService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

@Tag(name = "Admin Order SSE Controller")
@RestController
@RequestMapping("/admin/sse")
class AdminOrderSseController(
    private val sseEmitterService: SseEmitterService,
    private val workspaceService: WorkspaceService,
) {
    @Operation(summary = "주문 SSE 구독", description = "워크스페이스의 주문 이벤트를 SSE로 수신합니다.")
    @GetMapping("/orders/{workspaceId}", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun subscribeOrders(
        @AdminUsername username: String,
        @PathVariable workspaceId: Long,
    ): SseEmitter {
        if (!workspaceService.isAccessible(username, workspaceId)) throw WorkspaceInaccessibleException()

        val emitter = SseEmitter(Long.MAX_VALUE)
        sseEmitterService.addEmitter(workspaceId, emitter)

        emitter.onCompletion { sseEmitterService.removeEmitter(workspaceId, emitter) }
        emitter.onTimeout { sseEmitterService.removeEmitter(workspaceId, emitter) }
        emitter.onError { sseEmitterService.removeEmitter(workspaceId, emitter) }

        emitter.send(SseEmitter.event().name("connected").data("connected"))

        return emitter
    }
}
