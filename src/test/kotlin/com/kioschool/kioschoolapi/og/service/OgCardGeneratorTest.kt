package com.kioschool.kioschoolapi.og.service

import com.kioschool.kioschoolapi.global.og.service.OgCardGenerator
import com.kioschool.kioschoolapi.global.aws.S3Service
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.assertThrows
import java.io.ByteArrayInputStream

class OgCardGeneratorTest : DescribeSpec({
    val s3Service = mockk<S3Service>()
    val sut = OgCardGenerator(s3Service, workspacePath = "test-path")

    fun loadFixture(name: String): ByteArray =
        OgCardGeneratorTest::class.java.getResourceAsStream("/og/$name")!!.readBytes()

    describe("generate") {
        it("uploads a 1200x630 PNG to the expected key and returns its URL") {
            val photoBytes = loadFixture("test-photo.jpg")
            val photoUrl = "https://bucket.s3.ap-northeast-2.amazonaws.com/foo.jpg"
            every { s3Service.downloadFileStream(photoUrl) } returns ByteArrayInputStream(photoBytes)
            val capturedBytes = slot<ByteArray>()
            val capturedPath = slot<String>()
            every { s3Service.uploadBytes(capture(capturedBytes), capture(capturedPath), "image/png") } returns
                "https://bucket.s3.ap-northeast-2.amazonaws.com/test-path/workspace42/og/HASH8CHR.png"

            val resultUrl = sut.generateUrl(workspaceId = 42L, sourcePhotoUrl = photoUrl)

            assert(capturedPath.captured.startsWith("test-path/workspace42/og/"))
            assert(capturedPath.captured.endsWith(".png"))
            assert(capturedBytes.captured.isNotEmpty())
            assert(resultUrl.contains("test-path/workspace42/og/"))
            verify { s3Service.uploadBytes(any(), any(), "image/png") }
        }

        it("uses sha1(sourceUrl).take(8) as the filename hash so different photos produce different keys") {
            val photoBytes = loadFixture("test-photo.jpg")
            every { s3Service.downloadFileStream(any()) } answers { ByteArrayInputStream(photoBytes) }
            val paths = mutableListOf<String>()
            every { s3Service.uploadBytes(any(), capture(paths), any()) } returns "url"

            sut.generateUrl(1L, "https://example/a.jpg")
            sut.generateUrl(1L, "https://example/b.jpg")

            assert(paths[0] != paths[1]) { "Different sources should yield different hashes" }
        }

        it("getExpectedUrl matches the path used by generate for the same input") {
            val photoBytes = loadFixture("test-photo.jpg")
            every { s3Service.downloadFileStream(any()) } returns ByteArrayInputStream(photoBytes)
            val uploadPath = slot<String>()
            every { s3Service.uploadBytes(any(), capture(uploadPath), any()) } returns "ignored"
            every { s3Service.getPublicUrl(any()) } answers { "computed:${firstArg<String>()}" }

            sut.generateUrl(7L, "https://example/x.jpg")
            val expected = sut.getExpectedUrl(7L, "https://example/x.jpg")

            verify { s3Service.getPublicUrl(uploadPath.captured) }
            assert(expected == "computed:${uploadPath.captured}")
        }

        it("propagates the exception when the source photo cannot be decoded as an image") {
            every { s3Service.downloadFileStream(any()) } returns ByteArrayInputStream(byteArrayOf(0, 1, 2, 3))

            assertThrows<Exception> {
                sut.generateUrl(1L, "https://example/broken")
            }
        }

        it("throws when the source photo exceeds MAX_SOURCE_BYTES (OOM guard)") {
            // MAX_SOURCE_BYTES + 1 만큼의 더미 byte. scrimage 디코딩 단계까진 가지 않아야 함.
            val oversized = ByteArray(OgCardGenerator.MAX_SOURCE_BYTES + 1)
            every { s3Service.downloadFileStream(any()) } returns ByteArrayInputStream(oversized)

            val ex = assertThrows<IllegalStateException> {
                sut.generateUrl(1L, "https://example/huge.jpg")
            }
            assert(ex.message!!.contains("exceeds size cap"))
        }
    }
})
