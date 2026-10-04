package com.kioschool.kioschoolapi.global.monitoring

import com.kioschool.kioschoolapi.domain.workspace.repository.WorkspaceTableRepository
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.mockk.every
import io.mockk.mockk
import org.springframework.web.socket.messaging.SubProtocolWebSocketHandler

class LiveOperationMetricsTest : DescribeSpec({
    val workspaceTableRepository = mockk<WorkspaceTableRepository>()
    val stats = mockk<SubProtocolWebSocketHandler.Stats> {
        every { webSocketSessions } returns 4
        every { httpStreamingSessions } returns 1
        every { httpPollingSessions } returns 2
    }
    val webSocketHandler = mockk<SubProtocolWebSocketHandler> {
        every { this@mockk.stats } returns stats
    }

    describe("LiveOperationMetrics") {
        val meterRegistry = SimpleMeterRegistry()
        val sut = LiveOperationMetrics(workspaceTableRepository, webSocketHandler, meterRegistry)

        it("publishes tables and workspaces in use after a refresh") {
            every { workspaceTableRepository.countByOrderSessionIsNotNull() } returns 37
            every { workspaceTableRepository.countDistinctWorkspaceByOrderSessionIsNotNull() } returns 6

            sut.refresh()

            meterRegistry.get("kioschool.tables.in.use").gauge().value() shouldBe 37.0
            meterRegistry.get("kioschool.workspaces.in.use").gauge().value() shouldBe 6.0
        }

        it("publishes websocket sessions per transport") {
            meterRegistry.get("kioschool.websocket.sessions").tag("transport", "websocket").gauge().value() shouldBe 4.0
            meterRegistry.get("kioschool.websocket.sessions").tag("transport", "http-streaming").gauge().value() shouldBe 1.0
            meterRegistry.get("kioschool.websocket.sessions").tag("transport", "http-polling").gauge().value() shouldBe 2.0
        }

        // 집계 쿼리가 실패해도 스케줄러 스레드를 죽이지 않고, 마지막 값을 유지한다
        it("keeps the last value when the count query fails") {
            every { workspaceTableRepository.countByOrderSessionIsNotNull() } throws RuntimeException("db down")

            sut.refresh()

            meterRegistry.get("kioschool.tables.in.use").gauge().value() shouldBe 37.0
        }
    }
})
