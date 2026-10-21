package com.kioschool.kioschoolapi.domain.order.repository

import com.kioschool.kioschoolapi.domain.order.entity.OrderProductComponent
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository

interface OrderProductComponentRepository : JpaRepository<OrderProductComponent, Long> {
    @EntityGraph(attributePaths = ["orderProduct", "orderProduct.order"])
    fun findWithOrderProductById(id: Long): OrderProductComponent?
}
