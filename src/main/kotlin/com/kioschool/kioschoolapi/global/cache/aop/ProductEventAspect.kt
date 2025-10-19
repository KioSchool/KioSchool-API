package com.kioschool.kioschoolapi.global.cache.aop

import com.kioschool.kioschoolapi.domain.product.entity.Product
import com.kioschool.kioschoolapi.domain.product.event.ProductUpdatedEvent
import org.aspectj.lang.annotation.AfterReturning
import org.aspectj.lang.annotation.Aspect
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component

@Aspect
@Component
class ProductEventAspect(
    private val eventPublisher: ApplicationEventPublisher
) {

    @AfterReturning(
        pointcut = "@annotation(com.kioschool.kioschoolapi.global.cache.annotation.ProductUpdateEvent)",
        returning = "product"
    )
    fun handleProductUpdate(product: Product) {
        eventPublisher.publishEvent(ProductUpdatedEvent(product.workspace.id))
    }
}
