package com.kioschool.kioschoolapi.global.cache.aop

import com.kioschool.kioschoolapi.domain.order.entity.Order
import com.kioschool.kioschoolapi.domain.order.event.OrderUpdatedEvent
import org.aspectj.lang.annotation.AfterReturning
import org.aspectj.lang.annotation.Aspect
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component

@Aspect
@Component
class OrderEventAspect(
    private val eventPublisher: ApplicationEventPublisher
) {

    @AfterReturning(pointcut = "@annotation(com.kioschool.kioschoolapi.global.cache.annotation.OrderUpdateEvent)", returning = "order")
    fun handleOrderUpdate(order: Order) {
        eventPublisher.publishEvent(OrderUpdatedEvent(order.id))
    }
}
