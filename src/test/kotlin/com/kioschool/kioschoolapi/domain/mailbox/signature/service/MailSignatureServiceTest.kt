package com.kioschool.kioschoolapi.domain.mailbox.signature.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.kioschool.kioschoolapi.domain.mailbox.service.MailHtmlSanitizer
import com.kioschool.kioschoolapi.domain.mailbox.signature.dto.MailSignatureBlock
import com.kioschool.kioschoolapi.domain.mailbox.signature.dto.MailSignatureContact
import com.kioschool.kioschoolapi.domain.mailbox.signature.dto.MailSignatureRequestBody
import com.kioschool.kioschoolapi.domain.mailbox.signature.dto.MailSignatureValues
import com.kioschool.kioschoolapi.domain.mailbox.signature.entity.MailSignature
import com.kioschool.kioschoolapi.domain.mailbox.signature.repository.MailSignatureRepository
import com.kioschool.kioschoolapi.domain.user.entity.User
import com.kioschool.kioschoolapi.global.common.enums.UserRole
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.*

class MailSignatureServiceTest : DescribeSpec({
    val repository = mockk<MailSignatureRepository>()
    val renderer = mockk<MailSignatureRenderer>()
    val mapper = ObjectMapper().findAndRegisterModules()
    val sut = MailSignatureService(repository, MailHtmlSanitizer(), renderer, mapper)

    val user = User(loginId = "admin", loginPassword = "p", name = "박지인", email = "a@kio-school.com", role = UserRole.SUPER_ADMIN, members = mutableListOf())
    val team = MailSignature(user = null, name = "키오스쿨 팀")
    val mine = MailSignature(user = user, name = "박지인")

    beforeTest {
        clearAllMocks()
        every { repository.findFirstByUserIsNull() } returns team
        every { repository.save(any<MailSignature>()) } answers { firstArg() }
        every { renderer.render(any()) } answers { MailSignatureBlock(firstArg<MailSignatureValues>().name.orEmpty(), "") }
    }

    describe("personalBlock") {
        it("내 설정이 있으면 내 서명을 쓴다") {
            every { repository.findByUser(user) } returns mine

            sut.personalBlock(user)!!.html shouldBe "박지인"
        }

        it("내 설정이 없으면 팀 공용을 쓴다") {
            every { repository.findByUser(user) } returns null

            sut.personalBlock(user)!!.html shouldBe "키오스쿨 팀"
        }
    }

    describe("saveMine") {
        it("처음 저장하면 내 설정을 새로 만들고, 인사말에서 서명 자리와 스크립트를 지운다") {
            every { repository.findByUser(user) } returns null
            val body = MailSignatureRequestBody(
                greetingHtml = """<p>안녕하세요<script>x</script></p><div data-mail-signature=""></div>""",
                name = " 박지인 ",
                englishName = "",
                contacts = listOf(MailSignatureContact("Email", "a@kio-school.com"), MailSignatureContact("Web", " ")),
            )

            val saved = sut.saveMine(user, body)

            saved.user shouldBe user
            saved.greetingHtml shouldBe "<p>안녕하세요</p>"
            saved.name shouldBe "박지인"
            saved.englishName shouldBe null
            saved.contactsJson shouldBe """[{"label":"Email","value":"a@kio-school.com"}]"""
        }

        it("글이 없는 인사말은 빈 값으로 저장한다") {
            every { repository.findByUser(user) } returns mine

            sut.saveMine(user, MailSignatureRequestBody(greetingHtml = "<p> </p>")).greetingHtml shouldBe ""
        }
    }

    describe("saveTeam") {
        it("팀 공용 한 줄을 고친다") {
            val current = MailSignature(user = null, name = "키오스쿨 팀")
            every { repository.findFirstByUserIsNull() } returns current

            sut.saveTeam(MailSignatureRequestBody(name = "키오스쿨")) shouldBe current
            current.name shouldBe "키오스쿨"
        }
    }
})
