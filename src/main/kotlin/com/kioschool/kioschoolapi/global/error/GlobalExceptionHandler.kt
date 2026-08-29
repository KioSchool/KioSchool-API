package com.kioschool.kioschoolapi.global.error

import com.kioschool.kioschoolapi.global.error.dto.ErrorResponse
import com.kioschool.kioschoolapi.global.error.dto.FieldErrorDetail
import com.kioschool.kioschoolapi.global.error.exception.CustomException
import com.kioschool.kioschoolapi.global.logging.annotation.Masked
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.ConstraintViolationException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.ServletWebRequest
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import java.time.Instant

@RestControllerAdvice
class GlobalExceptionHandler : ResponseEntityExceptionHandler() {
    private val log = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(CustomException::class)
    fun handleCustomException(
        ex: CustomException,
        request: HttpServletRequest,
    ): ResponseEntity<ErrorResponse> {
        val errorCode = ex.errorCode
        if (errorCode.status.is5xxServerError) {
            log.error("Server error [{}] at {}", errorCode.name, request.requestURI, ex)
        } else {
            log.warn("Business error [{}] at {}: {}", errorCode.name, request.requestURI, ex.message)
        }
        return ResponseEntity.status(errorCode.status)
            .body(ErrorResponse.of(errorCode, request.requestURI, ex.message, ex.errors))
    }

    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolation(
        ex: ConstraintViolationException,
        request: HttpServletRequest,
    ): ResponseEntity<ErrorResponse> {
        val errors = ex.constraintViolations.map {
            val masked = isMaskedField(it.rootBeanClass, it.propertyPath.toString())
            FieldErrorDetail(
                field = it.propertyPath.toString(),
                value = if (masked) null else it.invalidValue?.toString(),
                reason = it.message,
            )
        }
        log.warn("Constraint violation at {}: {}", request.requestURI, errors)
        return ResponseEntity.status(ErrorCode.INVALID_INPUT.status)
            .body(ErrorResponse.of(ErrorCode.INVALID_INPUT, request.requestURI, errors = errors))
    }

    @ExceptionHandler(Exception::class)
    fun handleUnexpected(
        ex: Exception,
        request: HttpServletRequest,
    ): ResponseEntity<ErrorResponse> {
        log.error("Unhandled exception at {}", request.requestURI, ex)
        return ResponseEntity.status(ErrorCode.INTERNAL_ERROR.status)
            .body(ErrorResponse.of(ErrorCode.INTERNAL_ERROR, request.requestURI))
    }

    // @Valid body validation failure → field-level errors[]
    // public (widened from protected) so unit tests can invoke it directly.
    public override fun handleMethodArgumentNotValid(
        ex: MethodArgumentNotValidException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest,
    ): ResponseEntity<Any>? {
        val path = request.servletPath()
        val targetClass = ex.bindingResult.target?.javaClass
        val errors = ex.bindingResult.fieldErrors.map {
            FieldErrorDetail(
                field = it.field,
                value = if (isMaskedField(targetClass, it.field)) null else it.rejectedValue?.toString(),
                reason = it.defaultMessage,
            )
        }
        log.warn("Validation failed at {}: {}", path, errors)
        return ResponseEntity.status(ErrorCode.INVALID_INPUT.status)
            .body(ErrorResponse.of(ErrorCode.INVALID_INPUT, path, errors = errors))
    }

    // All other Spring MVC framework exceptions (method not supported, media type,
    // unreadable body, missing param, ...) → preserve status, code = HTTP status name
    // public (widened from protected) so unit tests can invoke it directly.
    public override fun handleExceptionInternal(
        ex: Exception,
        body: Any?,
        headers: HttpHeaders,
        statusCode: HttpStatusCode,
        request: WebRequest,
    ): ResponseEntity<Any>? {
        val path = request.servletPath()
        val resolved = HttpStatus.resolve(statusCode.value())
        val code = resolved?.name ?: "ERROR"
        if (statusCode.is5xxServerError) {
            log.error("Framework exception [{}] at {}", code, path, ex)
        } else {
            log.warn("Framework exception [{}] at {}: {}", code, path, ex.message)
        }
        val errorResponse = ErrorResponse(
            code = code,
            message = resolved?.reasonPhrase ?: "error",
            status = statusCode.value(),
            path = path,
            timestamp = Instant.now(),
        )
        return ResponseEntity.status(statusCode).headers(headers).body(errorResponse)
    }

    private fun isMaskedField(targetClass: Class<*>?, fieldPath: String): Boolean {
        targetClass ?: return false
        val fieldName = fieldPath.substringAfterLast('.')
        return runCatching {
            targetClass.getDeclaredField(fieldName).isAnnotationPresent(Masked::class.java)
        }.getOrDefault(false)
    }

    private fun WebRequest.servletPath(): String =
        (this as? ServletWebRequest)?.request?.requestURI ?: ""
}
