package com.kioschool.kioschoolapi.aws

import com.amazonaws.services.s3.AmazonS3Client
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile

@Service
class S3Service(
    private val amazonS3Client: AmazonS3Client,
    @Value("\${cloud.aws.s3.bucket}")
    private val bucketName: String
) {

    fun uploadFile(file: MultipartFile, path: String): String {
        val extension = file.originalFilename
        amazonS3Client.putObject(bucketName, path, file.inputStream, null)
        return amazonS3Client.getUrl(bucketName, path).toString()
    }
}