package com.kioschool.kioschoolapi.error

import com.kioschool.kioschoolapi.global.error.ErrorCode
import com.kioschool.kioschoolapi.global.error.GlobalExceptionHandler
import com.kioschool.kioschoolapi.global.error.dto.ErrorResponse
import com.kioschool.kioschoolapi.global.error.exception.CustomException
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.ConstraintViolation
import jakarta.validation.ConstraintViolationException
import jakarta.validation.Path
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.validation.BindingResult
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.context.request.ServletWebRequest

class GlobalExceptionHandlerTest : DescribeSpec({
    val sut = GlobalExceptionHandler()

    fun request(uri: String): HttpServletRequest = mockk {
        every { requestURI } returns uri
    }

    describe("handleCustomException") {
        it("errorCode의 status/code/message로 응답한다") {
            val response = sut.handleCustomException(
                CustomException(ErrorCode.WORKSPACE_INACCESSIBLE),
                request("/admin/workspace/1"),
            )
            response.statusCode.value() shouldBe 405
            val body = response.body!!
            body.code shouldBe "WORKSPACE_INACCESSIBLE"
            body.message shouldBe "접근 불가능한 워크스페이스입니다."
            body.status shouldBe 405
            body.path shouldBe "/admin/workspace/1"
        }
    }

    describe("handleConstraintViolation") {
        it("INVALID_INPUT + 필드 errors로 응답한다") {
            // MockK cannot stub toString() on jakarta.validation.Path (it redeclares
            // toString() as an abstract interface method, and MockK still special-cases
            // Object-identity methods when recording `every {}`, throwing
            // "Missing mocked calls inside every { ... } block"). Use a tiny real
            // implementation instead of a mock for this one value.
            val path = object : Path {
                override fun iterator(): MutableIterator<Path.Node> = mutableListOf<Path.Node>().iterator()
                override fun toString(): String = "loginId"
            }
            val violation = mockk<ConstraintViolation<*>> {
                every { propertyPath } returns path
                every { invalidValue } returns ""
                every { message } returns "필수입니다."
            }
            val response = sut.handleConstraintViolation(
                ConstraintViolationException(setOf(violation)),
                request("/user/register"),
            )
            response.statusCode.value() shouldBe 400
            val body = response.body!!
            body.code shouldBe "INVALID_INPUT"
            body.errors.first().field shouldBe "loginId"
            body.errors.first().reason shouldBe "필수입니다."
        }
    }

    describe("handleUnexpected") {
        it("예상치 못한 예외는 INTERNAL_ERROR 500") {
            val response = sut.handleUnexpected(
                NoSuchElementException("boom"),
                request("/x"),
            )
            response.statusCode.value() shouldBe 500
            (response.body as ErrorResponse).code shouldBe "INTERNAL_ERROR"
        }
    }

    describe("handleMethodArgumentNotValid") {
        it("@Valid 실패를 INVALID_INPUT + errors[]로 변환한다") {
            val fieldError = org.springframework.validation.FieldError(
                "body", "name", "", false, null, null, "이름은 필수입니다.",
            )
            val bindingResult = mockk<BindingResult> {
                every { fieldErrors } returns listOf(fieldError)
            }
            val ex = mockk<MethodArgumentNotValidException> {
                every { this@mockk.bindingResult } returns bindingResult
            }
            val webRequest = ServletWebRequest(request("/user/register"))

            val response = sut.handleMethodArgumentNotValid(
                ex, HttpHeaders(), HttpStatus.BAD_REQUEST, webRequest,
            )

            response!!.statusCode.value() shouldBe 400
            val body = response.body as ErrorResponse
            body.code shouldBe "INVALID_INPUT"
            body.errors.first().field shouldBe "name"
            body.errors.first().reason shouldBe "이름은 필수입니다."
        }
    }
})
