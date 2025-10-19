package com.kioschool.kioschoolapi.global.cache.aop

import com.kioschool.kioschoolapi.domain.product.entity.ProductCategory
import com.kioschool.kioschoolapi.domain.product.event.ProductCategoryUpdatedEvent
import org.aspectj.lang.annotation.AfterReturning
import org.aspectj.lang.annotation.Aspect
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component

@Aspect
@Component
class ProductCategoryEventAspect(
    private val eventPublisher: ApplicationEventPublisher
) {

    @AfterReturning(pointcut = "@annotation(com.kioschool.kioschoolapi.global.cache.annotation.ProductCategoryUpdateEvent)", returning = "productCategory")
    fun handleProductCategoryUpdate(productCategory: ProductCategory) {
        eventPublisher.publishEvent(ProductCategoryUpdatedEvent(productCategory.workspace.id))
    }
}
