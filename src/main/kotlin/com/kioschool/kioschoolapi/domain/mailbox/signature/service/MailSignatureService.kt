package com.kioschool.kioschoolapi.domain.mailbox.signature.service

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.kioschool.kioschoolapi.domain.mailbox.service.MailHtmlSanitizer
import com.kioschool.kioschoolapi.domain.mailbox.signature.dto.MailSignatureBlock
import com.kioschool.kioschoolapi.domain.mailbox.signature.dto.MailSignatureContact
import com.kioschool.kioschoolapi.domain.mailbox.signature.dto.MailSignatureRequestBody
import com.kioschool.kioschoolapi.domain.mailbox.signature.dto.MailSignatureSettingDto
import com.kioschool.kioschoolapi.domain.mailbox.signature.dto.MailSignatureValues
import com.kioschool.kioschoolapi.domain.mailbox.signature.entity.MailSignature
import com.kioschool.kioschoolapi.domain.mailbox.signature.repository.MailSignatureRepository
import com.kioschool.kioschoolapi.domain.user.entity.User
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class MailSignatureService(
    private val mailSignatureRepository: MailSignatureRepository,
    private val mailHtmlSanitizer: MailHtmlSanitizer,
    private val mailSignatureRenderer: MailSignatureRenderer,
    private val objectMapper: ObjectMapper,
) {
    @Transactional(readOnly = true)
    fun findMine(user: User): MailSignature? = mailSignatureRepository.findByUser(user)

    /** 팀 공용은 배포 때 changelog가 넣어 둔다. 없으면 빈 값으로 본다. */
    @Transactional(readOnly = true)
    fun getTeam(): MailSignature = mailSignatureRepository.findFirstByUserIsNull() ?: MailSignature(user = null)

    /** 새 메일·답장에 넣을 서명. 내 설정이 없으면 팀 공용을 쓴다. */
    @Transactional(readOnly = true)
    fun personalBlock(user: User): MailSignatureBlock? = render(findMine(user) ?: getTeam())

    /** 여러 곳 보내기에 넣을 팀 공용 서명 */
    @Transactional(readOnly = true)
    fun teamBlock(): MailSignatureBlock? = render(getTeam())

    fun render(signature: MailSignature): MailSignatureBlock? = mailSignatureRenderer.render(valuesOf(signature))

    @Transactional
    fun saveMine(user: User, body: MailSignatureRequestBody): MailSignature =
        save(mailSignatureRepository.findByUser(user) ?: MailSignature(user = user), body)

    @Transactional
    fun saveTeam(body: MailSignatureRequestBody): MailSignature =
        save(mailSignatureRepository.findFirstByUserIsNull() ?: MailSignature(user = null), body)

    /** 탈퇴하면 서명에 넣은 이름·연락처도 지운다. */
    @Transactional
    fun deleteMine(user: User) = mailSignatureRepository.deleteByUser(user)

    fun cleanGreeting(greetingHtml: String): String {
        val cleaned = mailHtmlSanitizer.cleanWithoutSignature(greetingHtml)
        return if (mailHtmlSanitizer.isBlank(cleaned)) "" else cleaned
    }

    fun toDto(signature: MailSignature): MailSignatureSettingDto {
        val values = valuesOf(signature)
        return MailSignatureSettingDto(
            greetingHtml = signature.greetingHtml,
            name = values.name,
            englishName = values.englishName,
            organization = values.organization,
            jobTitle = values.jobTitle,
            contacts = values.contacts,
            slogan = values.slogan,
            showLogo = values.showLogo,
        )
    }

    private fun save(signature: MailSignature, body: MailSignatureRequestBody): MailSignature {
        val values = body.toValues()
        signature.greetingHtml = cleanGreeting(body.greetingHtml)
        signature.name = values.name
        signature.englishName = values.englishName
        signature.organization = values.organization
        signature.jobTitle = values.jobTitle
        signature.contactsJson = objectMapper.writeValueAsString(values.contacts)
        signature.slogan = values.slogan
        signature.showLogo = values.showLogo
        return mailSignatureRepository.save(signature)
    }

    private fun valuesOf(signature: MailSignature) = MailSignatureValues(
        name = signature.name,
        englishName = signature.englishName,
        organization = signature.organization,
        jobTitle = signature.jobTitle,
        contacts = objectMapper.readValue(signature.contactsJson, CONTACTS_TYPE),
        slogan = signature.slogan,
        showLogo = signature.showLogo,
    ).normalized()

    companion object {
        private val CONTACTS_TYPE = object : TypeReference<List<MailSignatureContact>>() {}
    }
}
