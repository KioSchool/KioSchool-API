package com.kioschool.kioschoolapi.domain.order.repository

import com.kioschool.kioschoolapi.domain.order.entity.OrderProduct
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface OrderProductRepository : JpaRepository<OrderProduct, Long> {
    @EntityGraph(attributePaths = ["order"])
    fun findWithOrderById(id: Long): OrderProduct?

    // 세트 서빙 수는 구성품 값으로 계산해 저장하므로, 같은 세트를 동시에 서빙하면 서로의 변경을 못 보고 어긋난다.
    // 부모 행을 잠근 뒤 구성품을 읽게 해서 순서대로 처리한다.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select op from OrderProduct op where op.id = :id")
    fun findForUpdateById(@Param("id") id: Long): OrderProduct?
}
