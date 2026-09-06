package com.kioschool.kioschoolapi.domain.workspace.dto.common

import org.springframework.web.multipart.MultipartFile

/**
 * 대표 사진 슬롯 한 칸. 빈 슬롯은 아예 만들지 않는다.
 * focalPoint가 null이면 "이번 요청에 지정이 없다"는 뜻이며,
 * 기존 이미지는 저장된 값을 유지하고 신규 파일만 기본값을 쓴다.
 */
sealed class WorkspaceImageSlot {
    abstract val focalPoint: FocalPointDto?

    data class Existing(
        val imageId: Long,
        override val focalPoint: FocalPointDto?,
    ) : WorkspaceImageSlot()

    data class New(
        val file: MultipartFile,
        override val focalPoint: FocalPointDto?,
    ) : WorkspaceImageSlot()
}
