package com.kioschool.kioschoolapi.og.facade

import com.kioschool.kioschoolapi.domain.workspace.entity.Workspace
import com.kioschool.kioschoolapi.domain.workspace.entity.WorkspaceSetting
import com.kioschool.kioschoolapi.domain.workspace.service.WorkspaceService
import com.kioschool.kioschoolapi.factory.SampleEntity
import com.kioschool.kioschoolapi.global.common.entity.BaseEntity
import com.kioschool.kioschoolapi.global.og.facade.OgFacade
import com.kioschool.kioschoolapi.global.og.facade.ShareLinkAction
import com.kioschool.kioschoolapi.global.og.service.OgService
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlin.reflect.full.superclasses

class OgFacadeTest : DescribeSpec({
    val workspaceService = mockk<WorkspaceService>()
    val ogService = mockk<OgService>()
    val sut = OgFacade(workspaceService, ogService, baseUrl = "https://kio-school.com")

    beforeEach { clearMocks(workspaceService, ogService) }

    fun BaseEntity.setBaseId(id: Long) {
        val f = this::class.superclasses.first().java.getDeclaredField("id")
        f.isAccessible = true
        f.set(this, id)
    }

    fun newWorkspace(workspaceId: Long): Workspace {
        val ws = Workspace(
            name = "test",
            owner = SampleEntity.user,
            workspaceSetting = WorkspaceSetting(),
        )
        ws.setBaseId(workspaceId)
        return ws
    }

    describe("renderOrderHtml") {
        it("looks up the workspace and forwards both to OgService") {
            val ws = newWorkspace(1L)
            every { workspaceService.findWorkspaceOrNull(1L) } returns ws
            every { ogService.renderOrderHtmlFor(ws, 1L) } returns "<html>ok</html>"

            assert(sut.renderOrderHtml(1L) == "<html>ok</html>")

            verify(exactly = 1) { workspaceService.findWorkspaceOrNull(1L) }
            verify(exactly = 1) { ogService.renderOrderHtmlFor(ws, 1L) }
        }

        it("forwards null workspace when workspaceId is null") {
            every { ogService.renderOrderHtmlFor(null, null) } returns "<html>fallback</html>"

            assert(sut.renderOrderHtml(null) == "<html>fallback</html>")

            verify(exactly = 0) { workspaceService.findWorkspaceOrNull(any()) }
            verify(exactly = 1) { ogService.renderOrderHtmlFor(null, null) }
        }

        it("forwards null workspace when workspaceService returns null for unknown id") {
            every { workspaceService.findWorkspaceOrNull(999L) } returns null
            every { ogService.renderOrderHtmlFor(null, 999L) } returns "<html>fallback</html>"

            assert(sut.renderOrderHtml(999L) == "<html>fallback</html>")
        }
    }

    describe("resolveShareLink — bot branch") {
        val botUas = listOf(
            "Mozilla/5.0 (compatible; KAKAOTALK 9.0.0)",
            "facebookexternalhit/1.1",
            "Slackbot-LinkExpanding 1.0",
            "Mozilla/5.0 (compatible; Googlebot/2.1)",
            "Twitterbot/1.0",
            "Mozilla/5.0 (compatible; bingbot/2.0)",
        )

        botUas.forEach { ua ->
            it("returns RenderOgHtml for bot UA: ${ua.take(40)}") {
                val ws = newWorkspace(42L)
                every { workspaceService.findWorkspaceOrNull(42L) } returns ws
                every { ogService.renderOrderHtmlFor(ws, 42L) } returns "<html>og</html>"

                val action = sut.resolveShareLink(42L, tableNo = null, tableHash = null, userAgent = ua)

                assert(action is ShareLinkAction.RenderOgHtml)
                assert((action as ShareLinkAction.RenderOgHtml).body == "<html>og</html>")
            }
        }

        it("og card is workspace-level — table params are ignored on bot branch") {
            val ws = newWorkspace(42L)
            every { workspaceService.findWorkspaceOrNull(42L) } returns ws
            every { ogService.renderOrderHtmlFor(ws, 42L) } returns "<html>og</html>"

            val action = sut.resolveShareLink(
                workspaceId = 42L,
                tableNo = 3,
                tableHash = "abc123",
                userAgent = "KAKAOTALK",
            )

            assert(action is ShareLinkAction.RenderOgHtml)
            // 봇 브랜치에선 renderOrderHtmlFor가 workspaceId만 인자로 받음 (table 정보 미전달)
            verify(exactly = 1) { ogService.renderOrderHtmlFor(ws, 42L) }
        }
    }

    describe("resolveShareLink — human branch") {
        val humanUas = listOf(
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 Chrome/120.0.0.0 Safari/537.36",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 Safari/604.1",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Firefox/120.0",
        )

        humanUas.forEach { ua ->
            it("returns RedirectToOrder for human UA: ${ua.take(40)}") {
                val action = sut.resolveShareLink(42L, tableNo = null, tableHash = null, userAgent = ua)

                assert(action is ShareLinkAction.RedirectToOrder)
                val target = (action as ShareLinkAction.RedirectToOrder).target.toString()
                assert(target == "https://kio-school.com/order?workspaceId=42") {
                    "Expected redirect to canonical order URL, got: $target"
                }
                verify(exactly = 0) { ogService.renderOrderHtmlFor(any(), any()) }
            }
        }

        it("treats missing UA as human") {
            val action = sut.resolveShareLink(42L, null, null, userAgent = null)

            assert(action is ShareLinkAction.RedirectToOrder)
        }

        it("treats blank UA as human") {
            val action = sut.resolveShareLink(42L, null, null, userAgent = "")

            assert(action is ShareLinkAction.RedirectToOrder)
        }

        it("uses configured baseUrl for the redirect target") {
            val devSut = OgFacade(workspaceService, ogService, baseUrl = "https://dev.kio-school.com")

            val action = devSut.resolveShareLink(7L, null, null, userAgent = "Chrome")

            val target = (action as ShareLinkAction.RedirectToOrder).target.toString()
            assert(target == "https://dev.kio-school.com/order?workspaceId=7")
        }

        it("preserves tableNo and tableHash in the redirect target") {
            val action = sut.resolveShareLink(
                workspaceId = 42L,
                tableNo = 3,
                tableHash = "abc123",
                userAgent = "Chrome",
            )

            val target = (action as ShareLinkAction.RedirectToOrder).target.toString()
            assert(target == "https://kio-school.com/order?workspaceId=42&tableNo=3&tableHash=abc123") {
                "Expected redirect to include both table params, got: $target"
            }
        }

        it("preserves only tableNo when tableHash is absent") {
            val action = sut.resolveShareLink(42L, tableNo = 3, tableHash = null, userAgent = "Chrome")

            val target = (action as ShareLinkAction.RedirectToOrder).target.toString()
            assert(target == "https://kio-school.com/order?workspaceId=42&tableNo=3")
        }

        it("preserves only tableHash when tableNo is absent") {
            val action = sut.resolveShareLink(42L, tableNo = null, tableHash = "abc123", userAgent = "Chrome")

            val target = (action as ShareLinkAction.RedirectToOrder).target.toString()
            assert(target == "https://kio-school.com/order?workspaceId=42&tableHash=abc123")
        }

        it("URL-encodes tableHash to handle special characters safely") {
            val action = sut.resolveShareLink(
                workspaceId = 42L,
                tableNo = null,
                tableHash = "hash with spaces & symbols",
                userAgent = "Chrome",
            )

            val target = (action as ShareLinkAction.RedirectToOrder).target.toString()
            // UriComponentsBuilder가 encoding 처리
            assert(!target.contains("hash with spaces")) { "Expected encoded output, got: $target" }
            assert(target.contains("workspaceId=42"))
            assert(target.contains("tableHash="))
        }
    }
})
