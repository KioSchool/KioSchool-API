package com.kioschool.kioschoolapi.domain.inquiry.dto.common

import com.kioschool.kioschoolapi.domain.inquiry.entity.InquiryImage

data class InquiryImageDto(
    val id: Long,
    val originalFileName: String,
    val contentType: String,
    val size: Long,
    val accessUrl: String,
) {
    companion object {
        // storageKey는 응답에 담지 않는다. accessUrl은 호출자가 계산해 넘긴다.
        fun of(image: InquiryImage, accessUrl: String) = InquiryImageDto(
            id = image.id,
            originalFileName = image.originalFileName,
            contentType = image.contentType,
            size = image.size,
            accessUrl = accessUrl,
        )
    }
}
