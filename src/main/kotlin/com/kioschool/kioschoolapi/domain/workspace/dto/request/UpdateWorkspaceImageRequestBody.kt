package com.kioschool.kioschoolapi.domain.workspace.dto.request

import com.kioschool.kioschoolapi.domain.workspace.dto.common.FocalPointDto
import com.kioschool.kioschoolapi.domain.workspace.dto.common.WorkspaceImageSlot
import com.kioschool.kioschoolapi.global.common.interfaces.WorkspaceAware
import com.kioschool.kioschoolapi.global.error.ErrorCode
import com.kioschool.kioschoolapi.global.error.exception.CustomException
import jakarta.validation.constraints.Size
import org.springframework.web.multipart.MultipartFile

class UpdateWorkspaceImageRequestBody(
    override val workspaceId: Long,
    @field:Size(min = 3, max = 3, message = "이미지 아이디는 3개여야 합니다.")
    val imageIds: List<Long?>,
    // 프론트보다 백엔드를 먼저 배포해도 깨지지 않도록 optional.
    @field:Size(min = 3, max = 3, message = "이미지 초점은 3개여야 합니다.")
    val focalPoints: List<FocalPointDto?>? = null,
) : WorkspaceAware {

    /**
     * imageFiles는 multipart라 null을 담을 수 없어, 프론트가 빈 슬롯을 뒤로 몰아 압축한
     * 순서로 파일만 보낸다. 슬롯 인덱스와 파일을 다시 맞춰야 초점이 올바른 사진에 붙는다.
     */
    fun toSlots(imageFiles: List<MultipartFile>): List<WorkspaceImageSlot> {
        validateFocalPoints()

        val fileIterator = imageFiles.iterator()
        val slots = mutableListOf<WorkspaceImageSlot>()

        imageIds.forEachIndexed { index, imageId ->
            val focalPoint = focalPoints?.getOrNull(index)
            when {
                imageId != null -> slots.add(WorkspaceImageSlot.Existing(imageId, focalPoint))
                fileIterator.hasNext() -> slots.add(
                    WorkspaceImageSlot.New(fileIterator.next(), focalPoint)
                )
            }
        }

        // 파일이 남았다는 건 압축 규칙이 깨졌다는 뜻이다. 그대로 두면 엉뚱한 사진에 초점이 붙는다.
        if (fileIterator.hasNext()) {
            throw CustomException(ErrorCode.WORKSPACE_IMAGE_SLOT_MISMATCH)
        }

        return slots
    }

    private fun validateFocalPoints() {
        focalPoints?.filterNotNull()?.forEach {
            if (!it.isInRange()) throw CustomException(ErrorCode.INVALID_IMAGE_FOCAL_POINT)
        }
    }
}
