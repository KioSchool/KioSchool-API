package com.kioschool.kioschoolapi.domain.inquiry.service

import com.kioschool.kioschoolapi.domain.inquiry.repository.InquiryRepository
import com.kioschool.kioschoolapi.global.aws.S3Service
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class InquiryPurgeService(
    private val inquiryRepository: InquiryRepository,
    private val s3Service: S3Service,
) {
    /**
     * 문의 한 건을 자기 트랜잭션에서 지운다.
     *
     * S3를 먼저 지우고 DB를 지운다. 순서를 뒤집으면 DB 기록이 사라진 뒤 S3 삭제가 실패했을 때
     * 고아 파일을 다시 찾을 방법이 없다. S3 삭제가 실패하면 트랜잭션이 롤백되어 행이 남고
     * 다음 실행에서 재시도된다. 이미 없는 객체 삭제는 성공으로 처리되므로 재실행이 멱등하다.
     */
    @Transactional(rollbackFor = [Exception::class])
    fun purgeOne(inquiryId: Long) {
        val inquiry = inquiryRepository.findByIdOrNull(inquiryId) ?: return

        inquiry.images.forEach { s3Service.deleteByKey(it.storageKey) }
        inquiryRepository.delete(inquiry)
    }
}
