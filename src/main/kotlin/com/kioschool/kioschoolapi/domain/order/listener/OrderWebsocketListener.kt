package com.kioschool.kioschoolapi.domain.order.listener

import com.kioschool.kioschoolapi.domain.order.dto.common.OrderDto
import com.kioschool.kioschoolapi.domain.order.event.OrderWebsocketEvent
import com.kioschool.kioschoolapi.domain.order.repository.OrderRepository
import com.kioschool.kioschoolapi.global.websocket.dto.Message
import com.kioschool.kioschoolapi.global.websocket.redis.RedisPublisher
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class OrderWebsocketListener(
    private val orderRepository: OrderRepository,
    private val redisPublisher: RedisPublisher
) {
    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(readOnly = true)
    fun handleOrderWebsocketEvent(event: OrderWebsocketEvent) {
        val order = orderRepository.findWithDetailsById(event.orderId) ?: return
        redisPublisher.publish(
            order.workspace.id,
            Message(event.type, OrderDto.of(order))
        )
    }
}