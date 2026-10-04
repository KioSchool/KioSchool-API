package com.kioschool.kioschoolapi.global.monitoring

import com.kioschool.kioschoolapi.domain.workspace.repository.WorkspaceTableRepository
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.messaging.SubProtocolWebSocketHandler
import java.util.concurrent.atomic.AtomicLong

/**
 * 지금 운영 중인 규모를 Grafana에서 보기 위한 게이지.
 *
 * - 사용 중 테이블·운영 중 주점: 주문 세션이 열린 테이블 수와 그 주점 수. 수집(15초)마다 DB를 치지 않게 30초마다 집계해 둔다.
 * - 웹소켓 연결: 이 인스턴스에 붙은 STOMP 세션(관리자 실시간 주문 화면) 수. 메모리 값이라 수집 때 바로 읽는다.
 */
@Component
class LiveOperationMetrics(
    private val workspaceTableRepository: WorkspaceTableRepository,
    @Qualifier("subProtocolWebSocketHandler")
    webSocketHandler: WebSocketHandler,
    meterRegistry: MeterRegistry,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val tablesInUse = AtomicLong()
    private val workspacesInUse = AtomicLong()

    init {
        Gauge.builder("kioschool.tables.in.use", tablesInUse) { it.get().toDouble() }
            .description("주문 세션이 열린 테이블 수")
            .register(meterRegistry)
        Gauge.builder("kioschool.workspaces.in.use", workspacesInUse) { it.get().toDouble() }
            .description("사용 중 테이블이 하나 이상인 주점 수")
            .register(meterRegistry)

        (webSocketHandler as? SubProtocolWebSocketHandler)?.let { handler ->
            mapOf<String, (SubProtocolWebSocketHandler.Stats) -> Int>(
                "websocket" to { it.webSocketSessions },
                "http-streaming" to { it.httpStreamingSessions },
                "http-polling" to { it.httpPollingSessions },
            ).forEach { (transport, count) ->
                Gauge.builder("kioschool.websocket.sessions", handler) { count(it.stats).toDouble() }
                    .description("이 인스턴스의 STOMP 세션 수 (SockJS 전송 방식별)")
                    .tag("transport", transport)
                    .register(meterRegistry)
            }
        }
    }

    @Scheduled(fixedRate = 30_000)
    fun refresh() {
        // 실패해도 스케줄러 스레드를 막지 않고 직전 값을 유지한다
        runCatching {
            tablesInUse.set(workspaceTableRepository.countByOrderSessionIsNotNull())
            workspacesInUse.set(workspaceTableRepository.countDistinctWorkspaceByOrderSessionIsNotNull())
        }.onFailure { log.warn("운영 현황 메트릭 집계 실패: {}", it.message) }
    }
}
