package com.kioschool.kioschoolapi.global.logging

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.kioschool.kioschoolapi.global.logging.aop.LoggingAspect
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.mockk.every
import io.mockk.mockk
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.reflect.MethodSignature
import org.slf4j.LoggerFactory

private data class CustomerOrder(val id: Long, val customerName: String)

class LoggingAspectMaskingTest : DescribeSpec({
    class SampleController

    describe("LoggingAspect") {
        val appender = ListAppender<ILoggingEvent>().apply { start() }
        (LoggerFactory.getLogger(LoggingAspect::class.java) as Logger).addAppender(appender)
        val sut = LoggingAspect(jacksonObjectMapper())

        it("masks personal arguments by parameter name and personal fields in the result") {
            val signature = mockk<MethodSignature>(relaxed = true) {
                every { declaringType } returns SampleController::class.java
                every { name } returns "sendCode"
                every { parameterNames } returns arrayOf("email", "workspaceId")
            }
            val joinPoint = mockk<ProceedingJoinPoint> {
                every { this@mockk.signature } returns signature
                every { args } returns arrayOf("kim@korea.ac.kr", 12L)
                every { proceed() } returns CustomerOrder(7, "홍길동")
            }

            sut.logExecutionTime(joinPoint)

            val logged = appender.list.joinToString("\n") { it.formattedMessage }
            logged shouldContain "****@korea.ac.kr"
            logged shouldContain "12"
            logged shouldContain """"customerName":"****""""
            logged shouldNotContain "kim@"
            logged shouldNotContain "홍길동"
        }
    }
})
