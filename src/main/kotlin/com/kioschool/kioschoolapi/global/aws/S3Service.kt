package com.kioschool.kioschoolapi.global.aws

import com.amazonaws.services.s3.AmazonS3Client
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile

@Service
class S3Service(
    private val amazonS3Client: AmazonS3Client,
    private val awsProperties: AwsProperties
) {

    fun uploadFile(file: MultipartFile, path: String): String {
        amazonS3Client.putObject(awsProperties.s3.bucket, path, file.inputStream, null)
        return amazonS3Client.getUrl(awsProperties.s3.bucket, path).toString()
    }

    fun deleteFile(url: String) {
        val path = url.split(awsProperties.s3.bucket).last()
        amazonS3Client.deleteObject(awsProperties.s3.bucket, path)
    }
}