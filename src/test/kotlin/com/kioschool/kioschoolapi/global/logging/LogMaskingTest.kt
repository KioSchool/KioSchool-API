package com.kioschool.kioschoolapi.global.logging

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.kioschool.kioschoolapi.global.logging.annotation.LogMasked
import com.kioschool.kioschoolapi.global.logging.annotation.Masked
import com.kioschool.kioschoolapi.global.logging.util.LogMasking
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain

private data class Account(val accountNumber: String, val accountHolder: String, val bankName: String)
private data class Operator(@LogMasked val name: String, val email: String?, val loginId: String, val account: Account?)
private data class OrderLine(val productName: String, val quantity: Int)
private data class Order(val id: Long, val customerName: String, val tableNumber: Int, val lines: List<OrderLine>)
private data class Product(val id: Long, val name: String)
private data class Login(val id: String, @Masked val password: String, @Masked val replyEmail: String)
private data class Flags(val hasEmail: Boolean, val ownerEmail: String)
private data class PageLike(val content: List<OrderLine>, val totalElements: Int)
private data class InquiryLike(val title: String, val content: String)

class LogMaskingTest : DescribeSpec({
    val base = jacksonObjectMapper()
    val mapper = LogMasking.loggingMapper(base)

    describe("loggingMapper") {
        it("masks personal fields in nested objects and lists, keeping ids and product names") {
            val json = mapper.writeValueAsString(
                listOf(Order(7, "홍길동", 3, listOf(OrderLine("닭강정", 2))))
            )

            json shouldBe """[{"id":7,"customerName":"****","tableNumber":3,"lines":[{"productName":"닭강정","quantity":2}]}]"""
        }

        it("keeps the email domain, masks the account and names marked @LogMasked") {
            val json = mapper.writeValueAsString(
                Operator("김운영", "kim@gs.anyang.ac.kr", "kimop", Account("1002-123-456789", "김운영", "우리은행"))
            )

            json shouldBe """{"name":"****","email":"****@gs.anyang.ac.kr","loginId":"kimop","account":{"accountNumber":"****","accountHolder":"****","bankName":"우리은행"}}"""
        }

        it("leaves null personal fields as null") {
            mapper.writeValueAsString(Operator("김운영", null, "kimop", null)) shouldContain """"email":null"""
        }

        it("does not mask a plain name without @LogMasked") {
            mapper.writeValueAsString(Product(1, "모듬 꼬치")) shouldBe """{"id":1,"name":"모듬 꼬치"}"""
        }

        it("keeps @Masked fields fully masked") {
            mapper.writeValueAsString(Login("kimop", "secret", "kim@korea.ac.kr")) shouldBe
                """{"id":"kimop","password":"****","replyEmail":"****"}"""
        }

        it("masks only string email properties") {
            mapper.writeValueAsString(Flags(true, "owner@korea.ac.kr")) shouldBe
                """{"hasEmail":true,"ownerEmail":"****@korea.ac.kr"}"""
        }

        // Spring Page의 content는 목록 자체다. 문의 본문 같은 문자열만 가린다
        it("masks free-text properties only when they are strings") {
            mapper.writeValueAsString(PageLike(listOf(OrderLine("닭강정", 2)), 1)) shouldBe
                """{"content":[{"productName":"닭강정","quantity":2}],"totalElements":1}"""
            mapper.writeValueAsString(InquiryLike("결제 문의", "계좌가 안 보여요")) shouldBe
                """{"title":"결제 문의","content":"****"}"""
        }

        it("does not change the original mapper") {
            val json = base.writeValueAsString(Order(7, "홍길동", 3, emptyList()))

            json shouldContain "홍길동"
            base.writeValueAsString(Operator("김운영", "kim@korea.ac.kr", "kimop", null)) shouldContain "김운영"
        }
    }

    describe("maskValue") {
        it("masks a bare argument by its parameter name") {
            LogMasking.maskValue("email", "kim@korea.ac.kr") shouldBe "****@korea.ac.kr"
            LogMasking.maskValue("customerName", "홍길동") shouldBe "****"
            LogMasking.maskValue("workspaceId", "12") shouldBe "12"
        }

        it("masks an email without a domain completely") {
            LogMasking.maskValue("email", "not-an-email") shouldBe "****"
        }

        it("handles field paths of validation errors") {
            LogMasking.maskValue("orders[0].customerName", "홍길동") shouldBe "****"
            LogMasking.maskValue("email", null) shouldBe null
        }
    }

    describe("maskEmail") {
        it("keeps only the domain") {
            LogMasking.maskEmail("someone@student.changwon.ac.kr") shouldBe "****@student.changwon.ac.kr"
            LogMasking.maskEmail("someone@student.changwon.ac.kr") shouldNotContain "someone"
        }
    }
})
