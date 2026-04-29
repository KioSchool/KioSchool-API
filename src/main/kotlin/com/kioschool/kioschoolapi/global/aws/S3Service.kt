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
        return java.net.URI(url).toURL().openStream()
    }

    fun deleteFile(url: String) {
        val path = url.split(bucketName).last()
        val objectKey = if (path.startsWith("/")) path.substring(1) else path
        amazonS3Client.deleteObject(bucketName, objectKey)
    }
}