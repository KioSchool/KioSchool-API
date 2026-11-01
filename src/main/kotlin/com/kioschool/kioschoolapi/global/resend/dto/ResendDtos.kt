package com.kioschool.kioschoolapi.global.resend.dto

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.OffsetDateTime

data class ResendCreateDraftRequest(
    val to: String,
    val subject: String,
    val html: String,
    val text: String,
)

data class ResendSendDraftRequest(
    val headers: Map<String, String>,
)

data class ResendReplyRequest(
    val html: String,
    val text: String,
)

data class ResendIdResponse(
    val id: String,
)

data class ResendListResponse<T>(
    @JsonProperty("has_more")
    val hasMore: Boolean,
    val data: List<T>,
)

data class ResendAttachment(
    val id: String,
    val filename: String?,
    val size: Long?,
)

data class ResendThreadEmail(
    val id: String,
    val direction: String,
    val from: String,
    val to: List<String>,
    val subject: String?,
    val html: String?,
    val text: String?,
    val attachments: List<ResendAttachment>,
    @JsonProperty("received_at")
    val receivedAt: OffsetDateTime,
)

data class ResendAttachmentDetail(
    @JsonProperty("download_url")
    val downloadUrl: String?,
)
