package com.kioschool.kioschoolapi.global.resend.api

import com.kioschool.kioschoolapi.global.resend.dto.*
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.*

interface ResendInboxApi {
    @POST("/inboxes/{inboxId}/drafts")
    fun createDraft(
        @Header("Authorization") authorization: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Path("inboxId") inboxId: String,
        @Body body: ResendCreateDraftRequest
    ): Call<ResendIdResponse>

    @POST("/inboxes/{inboxId}/drafts/{draftId}/send")
    fun sendDraft(
        @Header("Authorization") authorization: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Path("inboxId") inboxId: String,
        @Path("draftId") draftId: String,
        @Body body: ResendSendDraftRequest
    ): Call<ResendIdResponse>

    @POST("/inboxes/{inboxId}/threads/{threadId}/emails/{emailId}/reply")
    fun reply(
        @Header("Authorization") authorization: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Path("inboxId") inboxId: String,
        @Path("threadId") threadId: String,
        @Path("emailId") emailId: String,
        @Body body: ResendReplyRequest
    ): Call<ResendIdResponse>

    @GET("/inboxes/{inboxId}/threads/{threadId}/emails")
    fun listThreadEmails(
        @Header("Authorization") authorization: String,
        @Path("inboxId") inboxId: String,
        @Path("threadId") threadId: String,
        @Query("limit") limit: Int,
        @Query("after") after: String?
    ): Call<ResendListResponse<ResendThreadEmail>>

    @DELETE("/inboxes/{inboxId}/threads/{threadId}")
    fun deleteThread(
        @Header("Authorization") authorization: String,
        @Path("inboxId") inboxId: String,
        @Path("threadId") threadId: String
    ): Call<ResponseBody>

    @GET("/emails/receiving/{emailId}/attachments/{attachmentId}")
    fun getReceivedAttachment(
        @Header("Authorization") authorization: String,
        @Path("emailId") emailId: String,
        @Path("attachmentId") attachmentId: String
    ): Call<ResendAttachmentDetail>
}
