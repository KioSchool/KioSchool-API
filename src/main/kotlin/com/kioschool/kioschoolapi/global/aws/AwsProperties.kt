package com.kioschool.kioschoolapi.global.aws

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "cloud.aws")
data class AwsProperties(
    val s3: S3,
    val credentials: Credentials,
    val region: Region
) {
    data class S3(
        val bucket: String,
        val defaultPath: String
    )

    data class Credentials(
        val accessKey: String,
        val secretKey: String
    )

    data class Region(
        val static: String
    )
}
