package com.kioschool.kioschoolapi.global.aws

import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.ObjectMetadata
import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.webp.WebpWriter
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.ByteArrayInputStream
import java.io.InputStream

@Service
class S3Service(
    private val amazonS3Client: AmazonS3Client,
    @Value("\${cloud.aws.s3.bucket}")
    private val bucketName: String
) {

    fun uploadFile(file: MultipartFile, path: String): String {
        amazonS3Client.putObject(bucketName, path, file.inputStream, null)
        return amazonS3Client.getUrl(bucketName, path).toString()
    }

    fun uploadResizedWebpImage(inputStream: InputStream, path: String, maxDimension: Int = 400): String {
        val image = ImmutableImage.loader().fromStream(inputStream)
        val webpBytes = image.max(maxDimension, maxDimension).bytes(WebpWriter.DEFAULT)

        val bais = ByteArrayInputStream(webpBytes)
        val metadata = ObjectMetadata().apply {
            contentLength = webpBytes.size.toLong()
            contentType = "image/webp"
        }
        amazonS3Client.putObject(bucketName, path, bais, metadata)
        return amazonS3Client.getUrl(bucketName, path).toString()
    }

    fun downloadFileStream(url: String): InputStream {
        // S3 버킷/키에 의존하지 않고, 어떤 URL이든 직접 다운로드합니다. (단, 퍼블릭 접근이 가능한 URL이어야 함)
        // 타임아웃을 명시적으로 둬서 대용량 다운로드/지연된 응답이 worker 스레드를 무기한 점거하지 않도록 한다.
        val connection = java.net.URI(url).toURL().openConnection().apply {
            connectTimeout = DOWNLOAD_CONNECT_TIMEOUT_MS
            readTimeout = DOWNLOAD_READ_TIMEOUT_MS
        }
        return connection.getInputStream()
    }

    fun deleteFile(url: String) {
        val path = url.split(bucketName).last()
        val objectKey = if (path.startsWith("/")) path.substring(1) else path
        amazonS3Client.deleteObject(bucketName, objectKey)
    }

    fun uploadBytes(bytes: ByteArray, path: String, contentType: String): String {
        val metadata = ObjectMetadata().apply {
            contentLength = bytes.size.toLong()
            this.contentType = contentType
            cacheControl = "public, max-age=31536000, immutable"
        }
        amazonS3Client.putObject(
            bucketName,
            path,
            ByteArrayInputStream(bytes),
            metadata
        )
        return getPublicUrl(path)
    }

    /**
     * S3 키만으로 퍼블릭 URL을 계산한다 (PUT 없음). [OgCardGenerator.getExpectedUrl]이
     * hash 선검사할 때 — 즉 사진이 안 바뀌었으면 다운로드/합성/업로드를 모두 스킵하기 위해
     * — 사용한다. [uploadBytes]도 응답을 만들 때 이 메서드를 거쳐서 두 경로의 URL이
     * 항상 같은 형태로 나오도록 보장한다.
     */
    fun getPublicUrl(path: String): String =
        amazonS3Client.getUrl(bucketName, path).toString()

    companion object {
        const val DOWNLOAD_CONNECT_TIMEOUT_MS = 5_000
        const val DOWNLOAD_READ_TIMEOUT_MS = 30_000
    }
}
