package com.kioschool.kioschoolapi.global.aws

import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.ObjectMetadata
import com.kioschool.kioschoolapi.global.error.ErrorCode
import com.kioschool.kioschoolapi.global.error.exception.CustomException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.*
import org.springframework.mock.web.MockMultipartFile
import org.springframework.transaction.interceptor.DefaultTransactionAttribute
import java.io.InputStream
import java.net.URI

class S3ServiceTest : DescribeSpec({
    val amazonS3Client = mockk<AmazonS3Client>()
    val sut = S3Service(amazonS3Client, "test-bucket")

    afterTest { clearAllMocks() }

    describe("deleteByKey") {
        it("키를 그대로 써서 객체를 지운다") {
            every { amazonS3Client.deleteObject("test-bucket", "inquiry/inquiry-1/abc.png") } just Runs

            sut.deleteByKey("inquiry/inquiry-1/abc.png")

            verify(exactly = 1) {
                amazonS3Client.deleteObject("test-bucket", "inquiry/inquiry-1/abc.png")
            }
        }
    }

    describe("uploadMultipartFile") {
        it("contentLength와 contentType을 메타데이터에 담아 올리고 public URL을 돌려준다") {
            val file = MockMultipartFile("f", "shot.png", "image/png", ByteArray(10))
            val metadataSlot = slot<ObjectMetadata>()
            every {
                amazonS3Client.putObject("test-bucket", "inquiry/a.png", any<InputStream>(), capture(metadataSlot))
            } returns mockk()
            every {
                amazonS3Client.getUrl("test-bucket", "inquiry/a.png")
            } returns URI("https://test-bucket.s3.amazonaws.com/inquiry/a.png").toURL()

            val url = sut.uploadMultipartFile(file, "inquiry/a.png", "image/png")

            metadataSlot.captured.contentLength shouldBe 10L
            metadataSlot.captured.contentType shouldBe "image/png"
            url shouldBe "https://test-bucket.s3.amazonaws.com/inquiry/a.png"
        }
    }

    describe("uploadResizedWebpImage") {
        // 아이폰 기본 사진 포맷(HEIC). scrimage에 리더가 없어 어떤 리더로도 디코딩되지 않는다.
        fun heicBytes() = (byteArrayOf(0x00, 0x00, 0x00, 0x18) + "ftypheic".toByteArray() + ByteArray(64))

        it("디코딩할 수 없는 형식이면 UNSUPPORTED_IMAGE_FORMAT으로 바꿔 던진다") {
            val ex = shouldThrow<CustomException> {
                sut.uploadResizedWebpImage(heicBytes().inputStream(), "workspace1/product/a.webp")
            }

            ex.errorCode shouldBe ErrorCode.UNSUPPORTED_IMAGE_FORMAT
        }

        it("@Transactional 기본 규칙에서 롤백 대상이 된다") {
            // scrimage의 ImageParseException은 checked(IOException)라 Spring 기본 규칙
            // (RuntimeException/Error만 롤백)에 걸리지 않는다. 그대로 두면 주점 대표사진
            // 교체처럼 여러 장을 올리는 트랜잭션이 중간에 실패해도 커밋돼서,
            // 기존 사진은 지워지고 새 사진은 일부만 남는다.
            val ex = shouldThrow<CustomException> {
                sut.uploadResizedWebpImage(heicBytes().inputStream(), "workspace1/workspace/a.webp")
            }

            DefaultTransactionAttribute().rollbackOn(ex) shouldBe true
        }

        it("디코딩할 수 없는 형식이면 S3에 아무것도 올리지 않는다") {
            shouldThrow<CustomException> {
                sut.uploadResizedWebpImage(heicBytes().inputStream(), "workspace1/product/a.webp")
            }

            verify(exactly = 0) {
                amazonS3Client.putObject(any(), any(), any<InputStream>(), any())
            }
        }
    }
})
