package com.kioschool.kioschoolapi.domain.inquiry.util

import com.kioschool.kioschoolapi.global.error.ErrorCode
import com.kioschool.kioschoolapi.global.error.exception.CustomException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import org.springframework.mock.web.MockMultipartFile

class InquiryImageValidatorTest : DescribeSpec({

    val pngHeader = byteArrayOf(
        0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00, 0x00, 0x00
    )
    val jpegHeader = byteArrayOf(
        0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte(),
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
    )
    val webpHeader = "RIFF".toByteArray() + byteArrayOf(0x00, 0x00, 0x00, 0x00) + "WEBP".toByteArray()

    fun file(name: String, bytes: ByteArray, size: Long? = null) =
        object : MockMultipartFile(name, name, "image/png", bytes) {
            override fun getSize(): Long = size ?: bytes.size.toLong()
        }

    describe("detectContentType") {
        it("PNG 서명을 인식한다") {
            InquiryImageValidator.detectContentType(pngHeader) shouldBe "image/png"
        }

        it("JPEG 서명을 인식한다") {
            InquiryImageValidator.detectContentType(jpegHeader) shouldBe "image/jpeg"
        }

        it("WebP 서명을 인식한다") {
            InquiryImageValidator.detectContentType(webpHeader) shouldBe "image/webp"
        }

        it("SVG는 인식하지 않는다") {
            InquiryImageValidator.detectContentType("<svg xmlns=".toByteArray()) shouldBe null
        }

        it("GIF는 인식하지 않는다") {
            InquiryImageValidator.detectContentType("GIF89a......".toByteArray()) shouldBe null
        }

        it("12바이트보다 짧으면 인식하지 않는다") {
            InquiryImageValidator.detectContentType(byteArrayOf(0x89.toByte(), 0x50)) shouldBe null
        }
    }

    describe("validate") {
        it("이미지가 없으면 빈 목록을 돌려준다") {
            InquiryImageValidator.validate(emptyList()) shouldBe emptyList()
        }

        it("5장까지 허용한다") {
            val files = (1..5).map { file("a$it.png", pngHeader) }
            InquiryImageValidator.validate(files).size shouldBe 5
        }

        it("6장이면 INQUIRY_IMAGE_LIMIT_EXCEEDED") {
            val files = (1..6).map { file("a$it.png", pngHeader) }
            val ex = shouldThrow<CustomException> { InquiryImageValidator.validate(files) }
            ex.errorCode shouldBe ErrorCode.INQUIRY_IMAGE_LIMIT_EXCEEDED
        }

        it("5MB를 넘으면 INQUIRY_IMAGE_TOO_LARGE") {
            val files = listOf(file("a.png", pngHeader, size = 5L * 1024 * 1024 + 1))
            val ex = shouldThrow<CustomException> { InquiryImageValidator.validate(files) }
            ex.errorCode shouldBe ErrorCode.INQUIRY_IMAGE_TOO_LARGE
        }

        it("정확히 5MB이면 허용한다") {
            val files = listOf(file("a.png", pngHeader, size = InquiryImageValidator.MAX_IMAGE_SIZE_BYTES))
            InquiryImageValidator.validate(files).size shouldBe 1
        }

        it("확장자만 png인 HTML 파일은 INQUIRY_IMAGE_TYPE_NOT_ALLOWED") {
            val files = listOf(file("evil.png", "<html><script>".toByteArray()))
            val ex = shouldThrow<CustomException> { InquiryImageValidator.validate(files) }
            ex.errorCode shouldBe ErrorCode.INQUIRY_IMAGE_TYPE_NOT_ALLOWED
        }

        it("빈 파일은 INQUIRY_IMAGE_TYPE_NOT_ALLOWED") {
            val files = listOf(file("a.png", ByteArray(0)))
            val ex = shouldThrow<CustomException> { InquiryImageValidator.validate(files) }
            ex.errorCode shouldBe ErrorCode.INQUIRY_IMAGE_TYPE_NOT_ALLOWED
        }

        it("판별한 실제 형식과 확장자를 돌려준다") {
            val result = InquiryImageValidator.validate(listOf(file("shot.PNG", pngHeader)))
            result[0].contentType shouldBe "image/png"
            result[0].extension shouldBe "png"
        }

        it("JPEG 파일을 검증한다") {
            val result = InquiryImageValidator.validate(listOf(file("shot.jpg", jpegHeader)))
            result[0].contentType shouldBe "image/jpeg"
            result[0].extension shouldBe "jpeg"
        }

        it("WebP 파일을 검증한다") {
            val result = InquiryImageValidator.validate(listOf(file("shot.webp", webpHeader)))
            result[0].contentType shouldBe "image/webp"
            result[0].extension shouldBe "webp"
        }
    }
})
