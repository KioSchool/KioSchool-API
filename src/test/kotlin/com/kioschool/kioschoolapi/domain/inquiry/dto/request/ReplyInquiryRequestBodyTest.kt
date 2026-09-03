package com.kioschool.kioschoolapi.domain.inquiry.dto.request

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import jakarta.validation.Validation

class ReplyInquiryRequestBodyTest : DescribeSpec({
    val validator = Validation.buildDefaultValidatorFactory().validator

    describe("subject validation") {
        it("should accept a normal subject") {
            validator.validate(ReplyInquiryRequestBody("답변드립니다", "본문")).shouldBeEmpty()
        }
    }
})
