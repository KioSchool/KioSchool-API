package com.kioschool.kioschoolapi.domain.product.dto.request

import com.kioschool.kioschoolapi.global.common.interfaces.WorkspaceAware

// 모든 필드를 다 보낸다. components는 보낸 목록으로 통째로 바뀐다.
data class UpdateSetProductRequestBody(
    val productId: Long,
    override val workspaceId: Long,
    val name: String,
    val description: String,
    val productCategoryId: Long?,
    val originalPrice: Int,
    val discountPrice: Int,
    val components: List<SetComponentRequestBody>
) : WorkspaceAware
