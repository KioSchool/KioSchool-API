package com.kioschool.kioschoolapi.domain.mailbox.signature.dto

import com.kioschool.kioschoolapi.global.logging.annotation.LogMasked
import jakarta.validation.Valid
import jakarta.validation.constraints.Size

data class MailSignatureContact(
    @field:Size(max = 20, message = "연락처 이름표는 20자를 넘을 수 없습니다.")
    val label: String = "",

    @field:LogMasked
    @field:Size(max = 120, message = "연락처는 120자를 넘을 수 없습니다.")
    val value: String = "",
)

/** 서명을 그리는 데 필요한 값. 빈 글은 null로 바꿔 둔다. */
data class MailSignatureValues(
    val name: String? = null,
    val englishName: String? = null,
    val organization: String? = null,
    val jobTitle: String? = null,
    val contacts: List<MailSignatureContact> = emptyList(),
    val slogan: String? = null,
    val showLogo: Boolean = true,
) {
    fun normalized(): MailSignatureValues = MailSignatureValues(
        name = name.clean(),
        englishName = englishName.clean(),
        organization = organization.clean(),
        jobTitle = jobTitle.clean(),
        contacts = contacts
            .map { MailSignatureContact(it.label.trim(), it.value.trim()) }
            .filter { it.value.isNotEmpty() },
        slogan = slogan.clean(),
        showLogo = showLogo,
    )

    private fun String?.clean(): String? = this?.trim()?.takeIf { it.isNotEmpty() }
}

/** 메일 본문의 서명 자리에 넣을 HTML과 글 버전. 여러 곳 보내기는 만들 때 이 값을 저장해 둔다. */
data class MailSignatureBlock(
    val html: String = "",
    val text: String = "",
)

data class MailSignatureSettingDto(
    val greetingHtml: String,
    val name: String?,
    val englishName: String?,
    val organization: String?,
    val jobTitle: String?,
    val contacts: List<MailSignatureContact>,
    val slogan: String?,
    val showLogo: Boolean,
)

data class MailSettingsDto(
    /** 내 설정. 아직 저장한 적 없으면 null이고, 메일에는 팀 공용이 들어간다. */
    val mine: MailSignatureSettingDto?,
    val team: MailSignatureSettingDto,
    /** 새 메일·답장 편집기에 보여 줄 서명(내 설정, 없으면 팀 공용). 그릴 값이 없으면 null */
    val personalSignatureHtml: String?,
    /** 여러 곳 보내기 편집기에 보여 줄 팀 공용 서명. 그릴 값이 없으면 null */
    val teamSignatureHtml: String?,
    /** 내 설정이 없을 때 설정 화면의 이름 칸을 채울 계정 이름 */
    @field:LogMasked
    val myAccountName: String,
)

data class MailSignatureRequestBody(
    @field:Size(max = 5000, message = "인사말이 너무 깁니다.")
    val greetingHtml: String = "",

    @field:LogMasked
    @field:Size(max = 40, message = "이름은 40자를 넘을 수 없습니다.")
    val name: String? = null,

    @field:LogMasked
    @field:Size(max = 40, message = "영문 이름은 40자를 넘을 수 없습니다.")
    val englishName: String? = null,

    @field:Size(max = 40, message = "소속은 40자를 넘을 수 없습니다.")
    val organization: String? = null,

    @field:Size(max = 40, message = "직함은 40자를 넘을 수 없습니다.")
    val jobTitle: String? = null,

    @field:Valid
    @field:Size(max = 5, message = "연락처는 5줄까지 넣을 수 있습니다.")
    val contacts: List<MailSignatureContact> = emptyList(),

    @field:Size(max = 80, message = "한 줄 문구는 80자를 넘을 수 없습니다.")
    val slogan: String? = null,

    val showLogo: Boolean = true,
) {
    fun toValues(): MailSignatureValues =
        MailSignatureValues(name, englishName, organization, jobTitle, contacts, slogan, showLogo).normalized()
}
