package com.kioschool.kioschoolapi.domain.changelog.listener

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.kioschool.kioschoolapi.domain.changelog.entity.ChangeTargetType
import com.kioschool.kioschoolapi.domain.changelog.service.ChangeLogService
import com.kioschool.kioschoolapi.domain.order.event.OrderProductServedCountChangedEvent
import com.kioschool.kioschoolapi.domain.order.event.OrderSessionExpectedEndAtChangedEvent
import com.kioschool.kioschoolapi.domain.order.event.OrderStatusChangedEvent
import com.kioschool.kioschoolapi.domain.product.event.ProductChangedEvent
import com.kioschool.kioschoolapi.domain.product.event.ProductDeletedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// 동기 @EventListener라 발행한 쪽 트랜잭션 안에서 저장된다.
// @TransactionalEventListener는 트랜잭션이 없는 ProductFacade에서 발화하지 않아 쓰지 않는다.
// 같은 값인지는 ChangeLogService.record가 거른다.
@Component
class ChangeLogListener(
    private val changeLogService: ChangeLogService
) {
    private val mapper = jacksonObjectMapper()

    @EventListener
    fun handle(event: OrderStatusChangedEvent) {
        changeLogService.record(
            event.workspaceId,
            ChangeTargetType.ORDER,
            event.orderId,
            "status",
            event.before.name,
            event.after.name
        )
    }

    @EventListener
    fun handle(event: OrderProductServedCountChangedEvent) {
        changeLogService.record(
            event.workspaceId,
            ChangeTargetType.ORDER_PRODUCT,
            event.orderProductId,
            "served_count",
            event.before.toString(),
            event.after.toString()
        )
    }

    @EventListener
    fun handle(event: OrderSessionExpectedEndAtChangedEvent) {
        changeLogService.record(
            event.workspaceId,
            ChangeTargetType.ORDER_SESSION,
            event.orderSessionId,
            "expected_end_at",
            event.before?.let(::formatDateTime),
            event.after?.let(::formatDateTime)
        )
    }

    // 카테고리는 이번 기록 범위가 아니다. 스냅샷의 categoryId는 삭제 기록에만 쓴다.
    @EventListener
    fun handle(event: ProductChangedEvent) {
        val before = event.before
        val after = event.after
        recordProduct(event, "name", before.name, after.name)
        recordProduct(event, "price", before.price.toString(), after.price.toString())
        recordProduct(event, "status", before.status.name, after.status.name)
    }

    // 상품은 하드 삭제라 지운 뒤에는 원래 정보가 남지 않으므로 스냅샷을 통째로 남긴다.
    @EventListener
    fun handle(event: ProductDeletedEvent) {
        changeLogService.record(
            event.workspaceId,
            ChangeTargetType.PRODUCT,
            event.productId,
            "deleted",
            mapper.writeValueAsString(event.snapshot),
            null
        )
    }

    private fun recordProduct(event: ProductChangedEvent, field: String, oldValue: String, newValue: String) {
        changeLogService.record(event.workspaceId, ChangeTargetType.PRODUCT, event.productId, field, oldValue, newValue)
    }

    // LocalDateTime.toString()은 초가 0이면 생략해 값 형식이 섞이므로 초까지 고정한다.
    private fun formatDateTime(value: LocalDateTime): String = value.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
}
