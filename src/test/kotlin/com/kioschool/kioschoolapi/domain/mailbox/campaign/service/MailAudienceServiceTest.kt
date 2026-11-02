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

    fun filter(minOrders: Long = 15, schoolName: String? = null, excluded: Set<Long> = emptySet()) =
        MailAudienceFilter(from, to, minOrders, schoolName, excluded)

    fun stub(vararg workspaces: Workspace) {
        every { orderRepository.findWorkspaceOrderStats(any(), any(), any()) } returns workspaces.map {
            WorkspaceOrderStats(it.id, 20, 300000, 2, at, at.plusDays(1))
        }
        every { workspaceRepository.findAllById(any()) } returns workspaces.toList()
    }

    beforeTest {
        clearAllMocks()
        every { emailService.getSchoolResolver() } returns SchoolResolver(mapOf("konkuk.ac.kr" to "건국대학교", "sejong.ac.kr" to "세종대학교"))
        every { orderProductRepository.findMenuSales(any(), any(), any()) } returns emptyList()
        every { insightRepository.countByWorkspaceIds(any(), any(), any()) } returns emptyList()
        every { recipientRepository.findSentByWorkspaceIds(any()) } returns emptyList()
    }

    describe("resolve") {
        it("기간 안 주문 수가 minOrders 이상인 주점만 고른다. to 날짜의 주문도 포함한다") {
            stub(workspace(1))

            sut.resolve(filter(minOrders = 15)).map { it.workspaceId } shouldBe listOf(1L)

            verify {
                orderRepository.findWorkspaceOrderStats(from.atStartOfDay(), to.plusDays(1).atStartOfDay(), 15)
            }
        }

        it("대상 행에 주점과 사장 정보를 담는다") {
            stub(workspace(1))

            val row = sut.resolve(filter()).single()

            row.workspaceName shouldBe "주점1"
            row.schoolName shouldBe "건국대학교"
            row.ownerName shouldBe "사장1"
            row.ownerEmail shouldBe "o1@konkuk.ac.kr"
        }

        it("주문 수, 매출, 운영일, 테이블 수를 담는다") {
            stub(workspace(1))

            val row = sut.resolve(filter()).single()

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

            sut.resolve(filter()).single().topMenu shouldBe "김치전"
        }

        it("기간 안의 인사이트 카드 수를 센다") {
            stub(workspace(1))
            every { insightRepository.countByWorkspaceIds(listOf(1L), from, to) } returns listOf(arrayOf(1L, 3L))

            sut.resolve(filter()).single().insightCardCount shouldBe 3
        }

        it("가장 최근에 보낸 메일과 답장 여부를 담는다") {
            stub(workspace(1))
            every { recipientRepository.findSentByWorkspaceIds(listOf(1L)) } returns listOf(
                arrayOf(1L, "지난 설문", at, true),
                arrayOf(1L, "최근 설문", at.plusDays(5), false),
            )

            val row = sut.resolve(filter()).single()

            row.lastSentSubject shouldBe "최근 설문"
            row.lastSentAt shouldBe at.plusDays(5)
            row.lastReplied shouldBe false
        }

        it("보낸 적이 없으면 비워 둔다") {
            stub(workspace(1))

            val row = sut.resolve(filter()).single()

            row.lastSentAt shouldBe null
            row.insightCardCount shouldBe 0
            row.topMenu shouldBe null
        }

        it("schoolName이 있으면 그 학교 도메인만 고른다") {
            stub(workspace(1, email = "a@konkuk.ac.kr"), workspace(2, email = "b@sejong.ac.kr"))

            sut.resolve(filter(schoolName = "세종대학교")).map { it.workspaceId } shouldBe listOf(2L)
        }

        it("한 사장의 두 주점은 두 행으로 나온다") {
            val first = workspace(1)
            val second = Workspace(name = "주점2", owner = first.owner, workspaceSetting = WorkspaceSetting()).also {
                val f = BaseEntity::class.java.getDeclaredField("id")
                f.isAccessible = true
                f.set(it, 2L)
            }
            stub(first, second)

            sut.resolve(filter()).map { it.workspaceId } shouldBe listOf(1L, 2L)
        }

        it("주문이 있는 주점이 없으면 주점을 조회하지 않는다") {
            every { orderRepository.findWorkspaceOrderStats(any(), any(), any()) } returns emptyList()

            sut.resolve(filter()).size shouldBe 0

            verify(exactly = 0) { workspaceRepository.findAllById(any()) }
        }
    }

    describe("preview") {
        it("사장 이메일 없음, 탈퇴, 수신 거부는 빼고 사유별로 센다") {
            stub(
                workspace(1),
                workspace(2, email = null),
                workspace(3, email = "withdrawn-3@konkuk.ac.kr", withdrawnAt = at),
                workspace(4, unsubscribedAt = at),
                workspace(5, email = "  "),
            )

            val preview = sut.preview(filter(), 0, 20)

            preview.targetCount shouldBe 1
            preview.noEmailCount shouldBe 2
            preview.withdrawnCount shouldBe 1
            preview.unsubscribedCount shouldBe 1
            preview.dailyLimit shouldBe 70
            preview.content.content.map { it.workspaceId } shouldBe listOf(1L)
        }

        it("excludedWorkspaceIds는 빼지만 제외 사유 수에 넣지 않는다") {
            stub(workspace(1), workspace(2), workspace(3, email = null))

            val preview = sut.preview(filter(excluded = setOf(2, 3)), 0, 20)

            preview.targetCount shouldBe 1
            preview.noEmailCount shouldBe 0
        }

        it("페이지대로 잘라 돌려주고 전체 수를 유지한다") {
            stub(workspace(1), workspace(2), workspace(3))

            val preview = sut.preview(filter(), 1, 2)

            preview.targetCount shouldBe 3
            preview.content.content.map { it.workspaceId } shouldBe listOf(3L)
            preview.content.totalElements shouldBe 3
        }
    }
})
