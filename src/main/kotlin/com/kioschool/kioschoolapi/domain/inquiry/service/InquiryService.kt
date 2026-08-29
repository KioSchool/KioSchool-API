package com.kioschool.kioschoolapi.domain.inquiry.service

import com.kioschool.kioschoolapi.domain.email.service.EmailService
import com.kioschool.kioschoolapi.domain.inquiry.entity.Inquiry
import com.kioschool.kioschoolapi.domain.inquiry.entity.InquiryImage
import com.kioschool.kioschoolapi.domain.inquiry.repository.InquiryRepository
import com.kioschool.kioschoolapi.domain.inquiry.util.InquiryImageValidator
import com.kioschool.kioschoolapi.domain.user.service.UserService
import com.kioschool.kioschoolapi.global.aws.S3Service
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
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
    // 아래 둘은 Task 12(답변 발송)에서 쓴다. 생성자 시그니처를 두 번 바꾸지 않으려고 미리 받는다.
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

        // 명시적 재저장이 필요 없다: images는 cascade = ALL이고 이 메서드는 @Transactional이므로
        // 커밋 시점의 dirty checking으로 자동 반영된다. (managed 엔티티인 inquiry를 그대로 반환)
        return inquiry
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
}
