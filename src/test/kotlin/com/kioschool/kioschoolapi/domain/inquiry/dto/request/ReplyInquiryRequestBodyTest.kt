package com.kioschool.kioschoolapi.domain.inquiry.dto.request

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import jakarta.validation.Validation

class ReplyInquiryRequestBodyTest : DescribeSpec({
    val validator = Validation.buildDefaultValidatorFactory().validator

    describe("subject validation") {
        it("should reject a subject containing CRLF") {
            val violations = validator.validate(
                ReplyInquiryRequestBody("제목\r\nBcc: evil@example.com", "본문")
            )

            violations.size shouldBe 1
            violations.first().message shouldBe "답변 제목에 줄바꿈을 넣을 수 없습니다."
            violations.first().propertyPath.toString() shouldBe "subject"
        }

        it("should accept a normal subject") {
            validator.validate(ReplyInquiryRequestBody("답변드립니다", "본문")).shouldBeEmpty()
        }
    }
})
