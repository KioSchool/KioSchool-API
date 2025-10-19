package com.kioschool.kioschoolapi.domain.workspace.dto.common

import com.kioschool.kioschoolapi.domain.product.dto.common.ProductCategoryDto
import com.kioschool.kioschoolapi.domain.product.dto.common.ProductDto
import com.kioschool.kioschoolapi.domain.user.dto.common.UserDto
import com.kioschool.kioschoolapi.domain.workspace.entity.Workspace
import java.time.LocalDateTime

import com.kioschool.kioschoolapi.domain.workspace.dto.common.WorkspaceImageDto
import com.kioschool.kioschoolapi.domain.workspace.dto.common.WorkspaceSettingDto

data class WorkspaceDto(
    val id: Long,
    val name: String,
    val owner: UserDto,
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
        fun of(workspace: Workspace): WorkspaceDto {
            return WorkspaceDto(
                id = workspace.id,
                name = workspace.name,
                owner = UserDto.of(workspace.owner),
                products = workspace.products.map { ProductDto.of(it) },
                productCategories = workspace.productCategories.map { ProductCategoryDto.of(it) },
                images = workspace.images.map { WorkspaceImageDto.of(it) },
                description = workspace.description,
                notice = workspace.notice,
                tableCount = workspace.tableCount,
                workspaceSetting = WorkspaceSettingDto.of(workspace.workspaceSetting),
                createdAt = workspace.createdAt,
                updatedAt = workspace.updatedAt
            )
        }
    }
}
