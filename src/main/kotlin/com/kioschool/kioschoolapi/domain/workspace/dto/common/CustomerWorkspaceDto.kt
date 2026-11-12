package com.kioschool.kioschoolapi.domain.workspace.dto.common

import com.kioschool.kioschoolapi.domain.product.dto.common.ProductCategoryDto
import com.kioschool.kioschoolapi.domain.product.dto.common.ProductDto
import java.time.LocalDateTime

/** 로그인 없는 손님 주문 화면용. 주인 정보(owner)와 운영진 메모(memo)는 내보내지 않는다. */
data class CustomerWorkspaceDto(
    val id: Long,
    val name: String,
    val products: List<ProductDto>,
    val productCategories: List<ProductCategoryDto>,
    val images: List<WorkspaceImageDto>,
    val description: String,
    val notice: String,
    val tableCount: Int,
    val workspaceSetting: WorkspaceSettingDto,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?
) {
    companion object {
        fun of(workspace: WorkspaceDto): CustomerWorkspaceDto {
            return CustomerWorkspaceDto(
                id = workspace.id,
                name = workspace.name,
                products = workspace.products,
                productCategories = workspace.productCategories,
                images = workspace.images,
                description = workspace.description,
                notice = workspace.notice,
                tableCount = workspace.tableCount,
                workspaceSetting = workspace.workspaceSetting,
                createdAt = workspace.createdAt,
                updatedAt = workspace.updatedAt
            )
        }
    }
}
