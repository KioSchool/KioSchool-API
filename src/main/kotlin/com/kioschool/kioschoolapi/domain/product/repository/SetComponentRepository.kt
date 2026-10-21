package com.kioschool.kioschoolapi.domain.product.repository

import com.kioschool.kioschoolapi.domain.product.entity.SetComponent
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface SetComponentRepository : JpaRepository<SetComponent, Long> {
    fun findAllByConditionProductIdIn(productIds: Collection<Long>): List<SetComponent>
}
