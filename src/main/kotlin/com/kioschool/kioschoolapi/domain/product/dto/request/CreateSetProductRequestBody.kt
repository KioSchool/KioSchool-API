package com.kioschool.kioschoolapi.domain.product.dto.request

import com.kioschool.kioschoolapi.global.common.interfaces.WorkspaceAware

// 판매가(price)는 받지 않는다. originalPrice - discountPrice를 서버가 계산한다.
data class CreateSetProductRequestBody(
    override val workspaceId: Long,
    val name: String,
    val description: String,
    val productCategoryId: Long?,
    val originalPrice: Int,
    val discountPrice: Int,
    val components: List<SetComponentRequestBody>
) : WorkspaceAware
