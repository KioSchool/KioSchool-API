package com.kioschool.kioschoolapi.domain.inquiry.service

import com.kioschool.kioschoolapi.domain.email.service.EmailService
import com.kioschool.kioschoolapi.domain.inquiry.entity.Inquiry
import com.kioschool.kioschoolapi.domain.inquiry.entity.InquiryImage
import com.kioschool.kioschoolapi.domain.inquiry.entity.InquiryReply
import com.kioschool.kioschoolapi.domain.inquiry.enum.InquiryStatus
import com.kioschool.kioschoolapi.domain.inquiry.repository.InquiryRepository
import com.kioschool.kioschoolapi.domain.inquiry.util.InquiryImageValidator
import com.kioschool.kioschoolapi.domain.user.service.UserService
import com.kioschool.kioschoolapi.global.aws.S3Service
import com.kioschool.kioschoolapi.global.error.ErrorCode
import com.kioschool.kioschoolapi.global.error.exception.CustomException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.time.LocalDateTime
import java.util.*

@Service
class InquiryService(
    @Value("\${cloud.aws.s3.default-path}")
    private val defaultPath: String,
    @Value("\${inquiry.retention.unanswered-days}")
    private val unansweredRetentionDays: Long,
    @Value("\${inquiry.retention.resolved-days}")
    private val resolvedRetentionDays: Long,
    private val inquiryRepository: InquiryRepository,
    private val s3Service: S3Service,
    private val userService: UserService,
    private val emailService: EmailService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional(rollbackFor = [Exception::class])
    fun createInquiry(
        title: String,
        content: String,
        replyEmail: String,
        images: List<InquiryImageValidator.ValidatedImage>,
    ): Inquiry {
        val now = LocalDateTime.now()
        val inquiry = inquiryRepository.save(
            Inquiry(
                title = title,
                content = content,
                replyEmail = replyEmail,
                privacyAgreedAt = now,
                purgeAt = now.plusDays(unansweredRetentionDays),
            )
        )

        val uploadedKeys = mutableListOf<String>()
        registerRollbackCleanup(uploadedKeys)

        try {
            images.forEach { image ->
                val key = "$defaultPath/inquiry/inquiry-${inquiry.id}/${UUID.randomUUID()}.${image.extension}"
                s3Service.uploadMultipartFile(image.file, key, image.contentType)
                uploadedKeys.add(key)
                inquiry.images.add(
                    InquiryImage(
                        inquiry = inquiry,
                        storageKey = key,
                        originalFileName = image.file.originalFilename ?: "unknown",
                        contentType = image.contentType,
                        size = image.file.size,
                    )
                )
            }
        } catch (e: Exception) {
            deleteQuietly(uploadedKeys)
            throw e
        }

        return inquiry
    }

    fun getInquiries(status: InquiryStatus?, page: Int, size: Int): Page<Inquiry> {
        val pageable = PageRequest.of(
            page.coerceAtLeast(0),
            size.coerceIn(1, MAX_PAGE_SIZE),
            Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))
        )

        return if (status == null) inquiryRepository.findAll(pageable)
        else inquiryRepository.findAllByStatus(status, pageable)
    }

    fun getInquiry(inquiryId: Long): Inquiry =
        inquiryRepository.findByIdOrNull(inquiryId)
            ?: throw CustomException(ErrorCode.INQUIRY_NOT_FOUND)

    fun getImageAccessUrl(storageKey: String): String = s3Service.getPublicUrl(storageKey)

    /**
     * 잠금 → 상태 확인 → 동기 발송 → 저장을 한 트랜잭션에서 처리한다.
     * 발송이 실패하면 전체가 롤백되므로 실패한 답변이 DB에 남지 않는다.
     */
    @Transactional(rollbackFor = [Exception::class])
    fun replyToInquiry(
        username: String,
        inquiryId: Long,
        subject: String,
        content: String,
        emailBody: String,
    ): Inquiry {
        val inquiry = inquiryRepository.findByIdForUpdate(inquiryId)
            ?: throw CustomException(ErrorCode.INQUIRY_NOT_FOUND)
        validatePending(inquiry)

        val respondedBy = userService.getUser(username)

        // 수신 주소는 요청에서 받지 않는다. 반드시 문의에 저장된 값을 쓴다.
        emailService.sendEmailSync(inquiry.replyEmail, subject, emailBody)

        val sentAt = LocalDateTime.now()
        inquiry.reply = InquiryReply(
            inquiry = inquiry,
            subject = subject,
            content = content,
            recipientEmail = inquiry.replyEmail,
            respondedBy = respondedBy,
            sentAt = sentAt,
        )
        inquiry.status = InquiryStatus.ANSWERED
        inquiry.answeredAt = sentAt
        inquiry.purgeAt = sentAt.plusDays(resolvedRetentionDays)

        return inquiryRepository.save(inquiry)
    }

    @Transactional(rollbackFor = [Exception::class])
    fun closeInquiry(inquiryId: Long, closedReason: String?): Inquiry {
        val inquiry = inquiryRepository.findByIdForUpdate(inquiryId)
            ?: throw CustomException(ErrorCode.INQUIRY_NOT_FOUND)
        validatePending(inquiry)

        val closedAt = LocalDateTime.now()
        inquiry.status = InquiryStatus.CLOSED
        inquiry.closedAt = closedAt
        inquiry.closedReason = closedReason
        inquiry.purgeAt = closedAt.plusDays(resolvedRetentionDays)

        return inquiryRepository.save(inquiry)
    }

    private fun validatePending(inquiry: Inquiry) {
        val ignored: Unit = when (inquiry.status) {
            InquiryStatus.ANSWERED -> throw CustomException(ErrorCode.INQUIRY_ALREADY_ANSWERED)
            InquiryStatus.CLOSED -> throw CustomException(ErrorCode.INQUIRY_ALREADY_CLOSED)
            InquiryStatus.PENDING -> Unit
        }
    }

    /**
     * 업로드는 성공했는데 커밋이 실패하는 경우를 잡는다. 이게 없으면 DB에 기록이 없는
     * S3 파일이 영구히 남고 파기 스케줄러도 그것을 찾지 못한다.
     */
    private fun registerRollbackCleanup(uploadedKeys: List<String>) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) return

        TransactionSynchronizationManager.registerSynchronization(
            object : TransactionSynchronization {
                override fun afterCompletion(status: Int) {
                    if (status == TransactionSynchronization.STATUS_COMMITTED) return
                    deleteQuietly(uploadedKeys)
                }
            }
        )
    }

    private fun deleteQuietly(keys: List<String>) {
        keys.forEach { key ->
            runCatching { s3Service.deleteByKey(key) }
                .onFailure { log.error("Failed to delete orphaned inquiry image {}", key, it) }
        }
    }

    companion object {
        const val MAX_PAGE_SIZE = 100
    }
}
