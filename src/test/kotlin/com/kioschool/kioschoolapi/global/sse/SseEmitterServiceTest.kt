package com.kioschool.kioschoolapi.global.sse

import com.kioschool.kioschoolapi.global.websocket.dto.Message
import com.kioschool.kioschoolapi.global.common.enums.WebsocketType
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.*
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

class SseEmitterServiceTest : DescribeSpec({
    val sut = SseEmitterService()

    describe("addEmitter / removeEmitter") {
        it("emitter를 추가하면 workspaceId로 조회 가능하다") {
            val emitter = mockk<SseEmitter>(relaxed = true)
            sut.addEmitter(1L, emitter)
            sut.emitterCount(1L) shouldBe 1
            sut.removeEmitter(1L, emitter)
        }

        it("removeEmitter 후 목록에서 제거된다") {
            val emitter = mockk<SseEmitter>(relaxed = true)
            sut.addEmitter(1L, emitter)
            sut.removeEmitter(1L, emitter)
            sut.emitterCount(1L) shouldBe 0
        }
    }

    describe("sendToWorkspace") {
        it("해당 workspaceId의 모든 emitter에 메시지를 send한다") {
            val emitter1 = mockk<SseEmitter>(relaxed = true)
            val emitter2 = mockk<SseEmitter>(relaxed = true)
            sut.addEmitter(2L, emitter1)
            sut.addEmitter(2L, emitter2)

            val message = Message(WebsocketType.CREATED, "data")
            sut.sendToWorkspace(2L, message)

            verify(exactly = 1) { emitter1.send(any<SseEmitter.SseEventBuilder>()) }
            verify(exactly = 1) { emitter2.send(any<SseEmitter.SseEventBuilder>()) }

            sut.removeEmitter(2L, emitter1)
            sut.removeEmitter(2L, emitter2)
        }

        it("send 중 IOException 발생 시 해당 emitter를 목록에서 제거한다") {
            val brokenEmitter = mockk<SseEmitter>(relaxed = true)
            every { brokenEmitter.send(any<SseEmitter.SseEventBuilder>()) } throws java.io.IOException("broken")
            sut.addEmitter(3L, brokenEmitter)

            val message = Message(WebsocketType.CREATED, "data")
            sut.sendToWorkspace(3L, message)

            sut.emitterCount(3L) shouldBe 0
        }
    }
})
