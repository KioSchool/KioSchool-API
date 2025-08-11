package com.kioschool.kioschoolapi.factory

import com.kioschool.kioschoolapi.global.aws.AwsProperties

object SampleMock {
    val awsProperties = AwsProperties(
        s3 = AwsProperties.S3(
            bucket = "test-bucket",
            defaultPath = "images/test"
        ),
        credentials = AwsProperties.Credentials(
            accessKey = "test-access-key",
            secretKey = "test-secret-key"
        ),
        region = AwsProperties.Region(
            static = "ap-northeast-2"
        )
    )
}