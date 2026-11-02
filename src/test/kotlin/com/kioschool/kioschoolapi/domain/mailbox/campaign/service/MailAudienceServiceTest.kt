package com.kioschool.kioschoolapi.domain.mailbox.campaign.service

import com.kioschool.kioschoolapi.domain.email.service.EmailService
import com.kioschool.kioschoolapi.domain.email.service.SchoolResolver
import com.kioschool.kioschoolapi.domain.mailbox.campaign.dto.MailAudienceFilter
import com.kioschool.kioschoolapi.domain.insight.repository.DailyInsightCardRepository
import com.kioschool.kioschoolapi.domain.mailbox.campaign.repository.MailCampaignRecipientRepository
import com.kioschool.kioschoolapi.domain.order.dto.common.WorkspaceMenuSales
import com.kioschool.kioschoolapi.domain.order.dto.common.WorkspaceOrderStats
import com.kioschool.kioschoolapi.domain.order.repository.OrderProductRepository
import com.kioschool.kioschoolapi.domain.order.repository.OrderRepository
import com.kioschool.kioschoolapi.domain.user.entity.User
import com.kioschool.kioschoolapi.domain.workspace.entity.Workspace
import com.kioschool.kioschoolapi.domain.workspace.entity.WorkspaceSetting
import com.kioschool.kioschoolapi.domain.workspace.repository.WorkspaceRepository
import com.kioschool.kioschoolapi.global.common.entity.BaseEntity
import com.kioschool.kioschoolapi.global.common.enums.UserRole
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDate
import java.time.LocalDateTime

class MailAudienceServiceTest : DescribeSpec({
    val orderRepository = mockk<OrderRepository>()
    val orderProductRepository = mockk<OrderProductRepository>()
    val workspaceRepository = mockk<WorkspaceRepository>()
    val insightRepository = mockk<DailyInsightCardRepository>()
    val recipientRepository = mockk<MailCampaignRecipientRepository>()
    val emailService = mockk<EmailService>()
    val sut = MailAudienceService(orderRepository, orderProductRepository, workspaceRepository, insightRepository, recipientRepository, emailService, 70)
    val from = LocalDate.of(2026, 9, 1)
    val to = LocalDate.of(2026, 10, 31)
    val at = LocalDateTime.of(2026, 10, 1, 0, 0)

    fun workspace(
        id: Long,
        email: String? = "o$id@konkuk.ac.kr",
        withdrawnAt: LocalDateTime? = null,
        unsubscribedAt: LocalDateTime? = null,
    ): Workspace {
        val owner = User(
            loginId = "o$id", loginPassword = "p", name = "사장$id", email = email,
            role = UserRole.ADMIN, members = mutableListOf(),
        ).apply {
            this.withdrawnAt = withdrawnAt
            mailUnsubscribedAt = unsubscribedAt
        }
        return Workspace(name = "주점$id", owner = owner, workspaceSetting = WorkspaceSetting()).also {
            val f = BaseEntity::class.java.getDeclaredField("id")
            f.isAccessible = true
            f.set(it, id)
        }
    }

    fun filter(vararg ids: Long) = MailAudienceFilter(from, to, ids.toSet())

    fun stub(vararg workspaces: Workspace) {
        every { orderRepository.findWorkspaceOrderStats(any(), any()) } returns workspaces.map {
            WorkspaceOrderStats(it.id, 20, 300000, 2, at, at.plusDays(1))
        }
        every { workspaceRepository.findAllWithOwner() } returns workspaces.toList()
        every { workspaceRepository.findAllWithOwnerByIdIn(any()) } returns workspaces.toList()
    }

    beforeTest {
        clearAllMocks()
        every { emailService.getSchoolResolver() } returns SchoolResolver(mapOf("konkuk.ac.kr" to "건국대학교", "sejong.ac.kr" to "세종대학교"))
        every { orderProductRepository.findMenuSales(any(), any(), any()) } returns emptyList()
        every { insightRepository.countByWorkspaceIds(any(), any(), any()) } returns emptyList()
        every { recipientRepository.findSentByWorkspaceIds(any()) } returns emptyList()
    }

    describe("list") {
        it("주문이 없는 주점까지 모든 주점을 돌려주고 값은 0과 빈칸이 된다") {
            every { orderRepository.findWorkspaceOrderStats(any(), any()) } returns listOf(WorkspaceOrderStats(1, 3, 1000, 1, at, at))
            every { workspaceRepository.findAllWithOwner() } returns listOf(workspace(2), workspace(1))

            val rows = sut.list(from, to).rows

            rows.map { it.workspaceId } shouldBe listOf(1L, 2L)
            rows[0].orderCount shouldBe 3
            rows[1].orderCount shouldBe 0
            rows[1].revenue shouldBe 0
            rows[1].operatingDays shouldBe 0
            rows[1].firstOperatingDate shouldBe null
            rows[1].lastOperatingDate shouldBe null
        }

        it("사장 이메일 없음, 탈퇴, 수신 거부는 빼고 사유별로 센다") {
            stub(
                workspace(1),
                workspace(2, email = null),
                workspace(3, email = "withdrawn-3@konkuk.ac.kr", withdrawnAt = at),
                workspace(4, unsubscribedAt = at),
                workspace(5, email = "  "),
            )

            val audience = sut.list(from, to)

            audience.rows.map { it.workspaceId } shouldBe listOf(1L)
            audience.noEmailCount shouldBe 2
            audience.withdrawnCount shouldBe 1
            audience.unsubscribedCount shouldBe 1
            audience.dailyLimit shouldBe 70
        }

        it("한 사장의 두 주점은 두 행으로 나온다") {
            val first = workspace(1)
            val second = Workspace(name = "주점2", owner = first.owner, workspaceSetting = WorkspaceSetting()).also {
                val f = BaseEntity::class.java.getDeclaredField("id")
                f.isAccessible = true
                f.set(it, 2L)
            }
            stub(first, second)

            sut.list(from, to).rows.map { it.workspaceId } shouldBe listOf(1L, 2L)
        }
    }

    describe("resolve") {
        it("고른 주점만 돌려주고 끝 날짜의 주문까지 값에 넣는다") {
            every { orderRepository.findWorkspaceOrderStats(any(), any()) } returns listOf(
                WorkspaceOrderStats(1, 3, 1000, 1, at, at),
                WorkspaceOrderStats(2, 50, 900000, 3, at, at),
            )
            every { workspaceRepository.findAllWithOwnerByIdIn(setOf(1L)) } returns listOf(workspace(1))

            val row = sut.resolve(filter(1)).single()

            row.workspaceId shouldBe 1L
            row.orderCount shouldBe 3
            verify { orderRepository.findWorkspaceOrderStats(from.atStartOfDay(), to.plusDays(1).atStartOfDay()) }
        }

        it("기간 안에 주문이 없어도 돌려준다") {
            every { orderRepository.findWorkspaceOrderStats(any(), any()) } returns emptyList()
            every { workspaceRepository.findAllWithOwnerByIdIn(setOf(3L)) } returns listOf(workspace(3))

            sut.resolve(filter(3)).single().orderCount shouldBe 0
        }

        it("고른 주점이어도 탈퇴·이메일 없음·수신 거부는 받지 않는다") {
            stub(workspace(1), workspace(2, withdrawnAt = at), workspace(3, email = null), workspace(4, unsubscribedAt = at))

            sut.resolve(filter(1, 2, 3, 4)).map { it.workspaceId } shouldBe listOf(1L)
        }

        it("대상 행에 주점과 사장 정보를 담는다") {
            stub(workspace(1))

            val row = sut.resolve(filter(1)).single()

            row.workspaceName shouldBe "주점1"
            row.schoolName shouldBe "건국대학교"
            row.ownerName shouldBe "사장1"
            row.ownerEmail shouldBe "o1@konkuk.ac.kr"
        }

        it("주문 수, 매출, 운영일, 테이블 수를 담는다") {
            stub(workspace(1))

            val row = sut.resolve(filter(1)).single()

            row.orderCount shouldBe 20
            row.revenue shouldBe 300000
            row.operatingDays shouldBe 2
            row.firstOperatingDate shouldBe at.toLocalDate()
            row.lastOperatingDate shouldBe at.toLocalDate().plusDays(1)
            row.tableCount shouldBe 1
        }

        it("가장 많이 팔린 메뉴를 고른다") {
            stub(workspace(1))
            every { orderProductRepository.findMenuSales(listOf(1L), any(), any()) } returns listOf(
                WorkspaceMenuSales(1, "라면", 3),
                WorkspaceMenuSales(1, "김치전", 9),
            )

            sut.resolve(filter(1)).single().topMenu shouldBe "김치전"
        }

        it("기간 안의 인사이트 카드 수를 센다") {
            stub(workspace(1))
            every { insightRepository.countByWorkspaceIds(listOf(1L), from, to) } returns listOf(arrayOf(1L, 3L))

            sut.resolve(filter(1)).single().insightCardCount shouldBe 3
        }

        it("가장 최근에 보낸 메일과 답장 여부를 담는다") {
            stub(workspace(1))
            every { recipientRepository.findSentByWorkspaceIds(listOf(1L)) } returns listOf(
                arrayOf(1L, "지난 설문", at, true),
                arrayOf(1L, "최근 설문", at.plusDays(5), false),
            )

            val row = sut.resolve(filter(1)).single()

            row.lastSentSubject shouldBe "최근 설문"
            row.lastSentAt shouldBe at.plusDays(5)
            row.lastReplied shouldBe false
        }

        it("보낸 적이 없으면 비워 둔다") {
            stub(workspace(1))

            val row = sut.resolve(filter(1)).single()

            row.lastSentAt shouldBe null
            row.insightCardCount shouldBe 0
            row.topMenu shouldBe null
        }
    }
})
