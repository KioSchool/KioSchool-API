package com.kioschool.kioschoolapi.global.aws

import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.ObjectMetadata
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.*
import org.springframework.mock.web.MockMultipartFile
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
})
