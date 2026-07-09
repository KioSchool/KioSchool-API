package com.kioschool.kioschoolapi.global.monitoring

import com.zaxxer.hikari.HikariDataSource
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import javax.sql.DataSource

/**
 * 커넥션 풀이 압박을 받을 때만(가득 찼거나 대기 스레드가 있을 때) WARN 로그를 남긴다.
 *
 * 정상 상태에선 아무것도 찍지 않아 소음이 없고, 고갈이 시작되면 active/idle/waiting 추이가 로그에 남는다.
 * leak-detection(홀더 스택트레이스)과 함께 재발 시 원인 확정에 쓴다.
 * (DataSource는 datasource-micrometer의 ProxyDataSource로 감싸져 있어 unwrap으로 HikariDataSource를 꺼낸다.)
 */
@Component
class HikariPoolMonitor(dataSource: DataSource) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val hikari: HikariDataSource? =
        runCatching { dataSource.unwrap(HikariDataSource::class.java) }.getOrNull()

    init {
        if (hikari == null) {
            log.warn("HikariPoolMonitor: HikariDataSource unwrap 실패 — 풀 모니터링 비활성")
        } else {
            log.info("HikariPoolMonitor 활성 (maximumPoolSize={})", hikari.maximumPoolSize)
        }
    }

    @Scheduled(fixedRate = 10_000)
    fun monitor() {
        val pool = hikari?.hikariPoolMXBean ?: return
        val active = pool.activeConnections
        val total = pool.totalConnections
        val waiting = pool.threadsAwaitingConnection

        // 대기 스레드가 있거나 커넥션이 전부 사용 중일 때만 로그(정상일 땐 침묵)
        if (waiting > 0 || active >= total) {
            log.warn(
                "HikariPool 압박: active={}, idle={}, total={}, waiting={}",
                active, pool.idleConnections, total, waiting
            )
        }
    }
}
