package com.kioschool.kioschoolapi.domain.mailbox.signature.entity

import com.kioschool.kioschoolapi.domain.user.entity.User
import com.kioschool.kioschoolapi.global.common.entity.BaseEntity
import jakarta.persistence.*

/**
 * 메일 편집기에 미리 채우는 인사말과 서명 한 벌.
 * 주인([user])이 있으면 그 사람의 새 메일·답장에, 없으면 팀 공용으로 여러 곳 보내기와 설정이 없는 사람의 메일에 쓴다.
 */
@Entity
@Table(name = "mail_signature")
class MailSignature(
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    var user: User?,
    @Column(columnDefinition = "TEXT")
    var greetingHtml: String = "",
    var name: String? = null,
    var englishName: String? = null,
    var organization: String? = null,
    var jobTitle: String? = null,
    @Column(columnDefinition = "TEXT")
    var contactsJson: String = "[]",
    var slogan: String? = null,
    var showLogo: Boolean = true,
) : BaseEntity()
