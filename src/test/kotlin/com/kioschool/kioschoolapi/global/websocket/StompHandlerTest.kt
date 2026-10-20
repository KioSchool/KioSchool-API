package com.kioschool.kioschoolapi.global.websocket

import com.kioschool.kioschoolapi.domain.workspace.service.WorkspaceService
import com.kioschool.kioschoolapi.global.error.ErrorCode
import com.kioschool.kioschoolapi.global.error.exception.CustomException
import com.kioschool.kioschoolapi.global.security.JwtProvider
import com.kioschool.kioschoolapi.global.websocket.handler.StompHandler
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.messaging.support.MessageBuilder

class StompHandlerTest : DescribeSpec({
    val jwtProvider = mockk<JwtProvider>()
    val workspaceService = mockk<WorkspaceService>()
    val channel = mockk<MessageChannel>()

    val sut = StompHandler(jwtProvider, workspaceService)

    val token = "token"
    val username = "owner"

    fun frame(command: StompCommand, destination: String? = null): Message<ByteArray> {
        val accessor = StompHeaderAccessor.create(command)
        accessor.sessionAttributes = mutableMapOf<String, Any>("token" to token)
        destination?.let { accessor.destination = it }
        return MessageBuilder.createMessage(ByteArray(0), accessor.messageHeaders)
    }

    beforeTest {
        every { jwtProvider.isValidToken(token) } returns true
        every { jwtProvider.getLoginId(token) } returns username
        every { workspaceService.isAccessible(username, any()) } returns false
        every { workspaceService.isAccessible(username, 37L) } returns true
    }

    afterTest {
        clearAllMocks()
    }

    describe("SUBSCRIBE") {
        it("내 주점 주문 채널은 구독할 수 있다") {
            val message = frame(StompCommand.SUBSCRIBE, "/sub/order/37")

            sut.preSend(message, channel) shouldBe message
        }

        it("다른 주점 주문 채널은 거부한다") {
            val ex = shouldThrow<CustomException> {
                sut.preSend(frame(StompCommand.SUBSCRIBE, "/sub/order/5"), channel)
            }
            ex.errorCode shouldBe ErrorCode.WORKSPACE_INACCESSIBLE
        }

        listOf(
            "/sub/order/*",
            "/sub/order/37*",
            "/sub/order/37/**",
            "/sub/**/37",
            "/sub/order/37?",
            "/sub/order/",
            "/sub/order/99999999999999999999",
        ).forEach { destination ->
            it("/sub/order/{숫자} 형식이 아니면 거부한다: $destination") {
                val ex = shouldThrow<CustomException> {
                    sut.preSend(frame(StompCommand.SUBSCRIBE, destination), channel)
                }
                ex.errorCode shouldBe ErrorCode.WORKSPACE_INACCESSIBLE

                verify(exactly = 0) { workspaceService.isAccessible(any(), any()) }
            }
        }
    }

    describe("SEND") {
        it("클라이언트가 보내는 메시지는 받지 않는다") {
            val ex = shouldThrow<CustomException> {
                sut.preSend(frame(StompCommand.SEND, "/sub/order/37"), channel)
            }
            ex.errorCode shouldBe ErrorCode.ACCESS_DENIED
        }
    }
})
