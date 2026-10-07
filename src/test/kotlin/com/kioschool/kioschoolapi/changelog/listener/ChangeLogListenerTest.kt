package com.kioschool.kioschoolapi.changelog.listener

import com.kioschool.kioschoolapi.domain.changelog.entity.ChangeTargetType
import com.kioschool.kioschoolapi.domain.changelog.listener.ChangeLogListener
import com.kioschool.kioschoolapi.domain.changelog.service.ChangeLogService
import com.kioschool.kioschoolapi.domain.order.event.OrderProductServedCountChangedEvent
import com.kioschool.kioschoolapi.domain.order.event.OrderSessionExpectedEndAtChangedEvent
import com.kioschool.kioschoolapi.domain.order.event.OrderStatusChangedEvent
import com.kioschool.kioschoolapi.domain.product.event.ProductChangedEvent
import com.kioschool.kioschoolapi.domain.product.event.ProductDeletedEvent
import com.kioschool.kioschoolapi.domain.product.event.ProductSnapshot
import com.kioschool.kioschoolapi.global.common.enums.OrderStatus
import com.kioschool.kioschoolapi.global.common.enums.ProductStatus
import io.kotest.assertions.json.shouldEqualJson
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.*
import java.time.LocalDateTime

class ChangeLogListenerTest : DescribeSpec({
    val changeLogService = mockk<ChangeLogService>()

    val sut = ChangeLogListener(changeLogService)

    beforeTest {
        every { changeLogService.record(any(), any(), any(), any(), any(), any()) } just Runs
    }

    afterTest {
        clearAllMocks()
    }

    describe("OrderStatusChangedEvent") {
        it("should record the status change by enum name") {
            sut.handle(OrderStatusChangedEvent(1L, 10L, OrderStatus.PAID, OrderStatus.CANCELLED))

            verify(exactly = 1) {
                changeLogService.record(1L, ChangeTargetType.ORDER, 10L, "status", "PAID", "CANCELLED")
            }
        }
    }

    describe("OrderProductServedCountChangedEvent") {
        it("should record the served count change") {
            sut.handle(OrderProductServedCountChangedEvent(1L, 20L, 1, 2))

            verify(exactly = 1) {
                changeLogService.record(1L, ChangeTargetType.ORDER_PRODUCT, 20L, "served_count", "1", "2")
            }
        }
    }

    describe("OrderSessionExpectedEndAtChangedEvent") {
        it("should keep seconds in the ISO value even when they are zero") {
            sut.handle(
                OrderSessionExpectedEndAtChangedEvent(
                    1L,
                    30L,
                    LocalDateTime.of(2026, 10, 5, 21, 30),
                    LocalDateTime.of(2026, 10, 5, 22, 0)
                )
            )

            verify(exactly = 1) {
                changeLogService.record(
                    1L,
                    ChangeTargetType.ORDER_SESSION,
                    30L,
                    "expected_end_at",
                    "2026-10-05T21:30:00",
                    "2026-10-05T22:00:00"
                )
            }
        }

        it("should record null when the session had no expected end") {
            sut.handle(
                OrderSessionExpectedEndAtChangedEvent(1L, 30L, null, LocalDateTime.of(2026, 10, 5, 22, 0, 15))
            )

            verify(exactly = 1) {
                changeLogService.record(
                    1L,
                    ChangeTargetType.ORDER_SESSION,
                    30L,
                    "expected_end_at",
                    null,
                    "2026-10-05T22:00:15"
                )
            }
        }
    }

    describe("ProductChangedEvent") {
        it("should record name, price and status separately and leave the category out") {
            val before = ProductSnapshot("감자전", 5000, ProductStatus.SELLING, 3L)
            val after = ProductSnapshot("감자전(마감)", 3000, ProductStatus.SOLD_OUT, 4L)

            sut.handle(ProductChangedEvent(1L, 40L, before, after))

            verify(exactly = 1) {
                changeLogService.record(1L, ChangeTargetType.PRODUCT, 40L, "name", "감자전", "감자전(마감)")
            }
            verify(exactly = 1) {
                changeLogService.record(1L, ChangeTargetType.PRODUCT, 40L, "price", "5000", "3000")
            }
            verify(exactly = 1) {
                changeLogService.record(1L, ChangeTargetType.PRODUCT, 40L, "status", "SELLING", "SOLD_OUT")
            }
            verify(exactly = 3) { changeLogService.record(any(), any(), any(), any(), any(), any()) }
        }
    }

    describe("ProductDeletedEvent") {
        it("should record the snapshot as JSON in the old value") {
            val oldValue = slot<String>()
            every {
                changeLogService.record(1L, ChangeTargetType.PRODUCT, 40L, "deleted", capture(oldValue), isNull())
            } just Runs

            sut.handle(ProductDeletedEvent(1L, 40L, ProductSnapshot("감자전", 5000, ProductStatus.HIDDEN, null)))

            verify(exactly = 1) {
                changeLogService.record(1L, ChangeTargetType.PRODUCT, 40L, "deleted", any(), isNull())
            }
            oldValue.captured shouldEqualJson """{"name":"감자전","price":5000,"status":"HIDDEN","categoryId":null}"""
        }
    }
})
