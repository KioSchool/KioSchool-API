package com.kioschool.kioschoolapi.domain.mailbox.repository

import com.kioschool.kioschoolapi.domain.mailbox.entity.MailAttachment
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface MailAttachmentRepository : JpaRepository<MailAttachment, Long>
