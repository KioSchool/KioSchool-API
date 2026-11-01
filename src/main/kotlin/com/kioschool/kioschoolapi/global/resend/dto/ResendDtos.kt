package com.kioschool.kioschoolapi.global.resend.dto

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSetter
import com.fasterxml.jackson.annotation.Nulls
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
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

// 필드 하나가 없다고 대화 전체를 못 읽으면 메일을 잃으므로 id 말고는 없어도 읽는다
data class ResendThreadEmail(
    val id: String,
    @JsonSetter(nulls = Nulls.AS_EMPTY)
    val direction: String = "",
    @JsonSetter(nulls = Nulls.AS_EMPTY)
    val from: String = "",
    @JsonSetter(nulls = Nulls.AS_EMPTY)
    val to: List<String> = emptyList(),
    val subject: String? = null,
    val html: String? = null,
    val text: String? = null,
    @JsonSetter(nulls = Nulls.AS_EMPTY)
    val attachments: List<ResendAttachment> = emptyList(),
    @JsonProperty("received_at")
    @JsonAlias("created_at")
    @JsonDeserialize(using = ResendTimestampDeserializer::class)
    val receivedAt: OffsetDateTime = OffsetDateTime.now(),
)

data class ResendAttachmentDetail(
    @JsonProperty("download_url")
    val downloadUrl: String?,
)
