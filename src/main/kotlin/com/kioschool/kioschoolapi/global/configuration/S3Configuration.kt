package com.kioschool.kioschoolapi.global.configuration

import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.kioschool.kioschoolapi.global.aws.AwsProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration


@Configuration
class S3Configuration(
    private val awsProperties: AwsProperties
) {
    @Bean
    fun amazonS3Client(): AmazonS3Client {
        val credentials = BasicAWSCredentials(awsProperties.credentials.accessKey, awsProperties.credentials.secretKey)

        return AmazonS3ClientBuilder
            .standard()
            .withRegion(awsProperties.region.static)
            .withCredentials(AWSStaticCredentialsProvider(credentials))
            .build() as AmazonS3Client
    }
}