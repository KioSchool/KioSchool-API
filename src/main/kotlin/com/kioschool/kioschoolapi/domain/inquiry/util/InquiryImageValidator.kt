package com.kioschool.kioschoolapi.domain.inquiry.util

import com.kioschool.kioschoolapi.global.error.ErrorCode
import com.kioschool.kioschoolapi.global.error.exception.CustomException
import org.springframework.web.multipart.MultipartFile

/**
 * 문의 첨부 이미지 검증.
 *
 * 파일명 확장자와 요청 헤더의 Content-Type은 클라이언트가 정하는 문자열이라 신뢰하지 않는다.
 * 파일 앞 12바이트의 고정 서명(magic bytes)으로 실제 형식을 판별한다.
 *
 * 디코딩(scrimage) 방식은 쓰지 않는다. 인증 없는 공개 엔드포인트에서 압축 해제 시
 * 거대해지는 이미지(decompression bomb)를 받으면 OOM으로 서버가 죽는다.
 * 서명 검사는 앞 12바이트만 읽으므로 파일 크기와 무관하게 상수 시간이다.
 *
 * 다만 이 검증은 파일 앞부분의 서명만 확인할 뿐, 나머지 바이트에 대해서는 아무것도
 * 보장하지 않는다. 유효한 PNG/JPEG/WebP 서명 뒤에 임의의 바이트(HTML, 스크립트 등)를
 * 이어붙인 폴리글랏(polyglot) 파일도 이 검증을 통과한다. 따라서 이 검증을 통과한
 * 파일이라도 브라우저가 스니핑해 실행할 수 있는 Content-Type이나
 * `Content-Disposition: inline`으로 그대로 서빙해서는 안 되며, 저장 경로에
 * `file.originalFilename`을 신뢰해서도 안 된다(이후 저장 단계에서 UUID 기반 저장 키를
 * 생성하고, 파일명이 아닌 이 검증기가 판별한 실제 타입으로 S3 Content-Type을 명시적으로
 * 설정하는 것도 같은 이유다).
 */
object InquiryImageValidator {
    const val MAX_IMAGE_COUNT = 5
    const val MAX_IMAGE_SIZE_BYTES = 5L * 1024 * 1024
    private const val SIGNATURE_LENGTH = 12

    private val PNG_SIGNATURE = byteArrayOf(
        0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
    )
    private val JPEG_SIGNATURE = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())

    data class ValidatedImage(
        val file: MultipartFile,
        val contentType: String,
        val extension: String,
    )

    fun validate(files: List<MultipartFile>): List<ValidatedImage> {
        if (files.size > MAX_IMAGE_COUNT) {
            throw CustomException(ErrorCode.INQUIRY_IMAGE_LIMIT_EXCEEDED)
        }

        return files.map { file ->
            if (file.size > MAX_IMAGE_SIZE_BYTES) {
                throw CustomException(ErrorCode.INQUIRY_IMAGE_TOO_LARGE)
            }

            val header = file.inputStream.use { it.readNBytes(SIGNATURE_LENGTH) }
            val contentType = detectContentType(header)
                ?: throw CustomException(ErrorCode.INQUIRY_IMAGE_TYPE_NOT_ALLOWED)

            ValidatedImage(
                file = file,
                contentType = contentType,
                extension = contentType.substringAfter('/'),
            )
        }
    }

    fun detectContentType(header: ByteArray): String? {
        if (header.size < SIGNATURE_LENGTH) return null

        if (header.startsWith(PNG_SIGNATURE)) return "image/png"
        if (header.startsWith(JPEG_SIGNATURE)) return "image/jpeg"
        if (header.copyOfRange(0, 4).decodeToString() == "RIFF" &&
            header.copyOfRange(8, 12).decodeToString() == "WEBP"
        ) return "image/webp"

        return null
    }

    private fun ByteArray.startsWith(prefix: ByteArray): Boolean {
        if (size < prefix.size) return false
        return prefix.indices.all { this[it] == prefix[it] }
    }
}
