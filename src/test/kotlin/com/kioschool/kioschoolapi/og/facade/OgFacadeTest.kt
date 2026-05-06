package com.kioschool.kioschoolapi.og.facade

import com.kioschool.kioschoolapi.domain.workspace.entity.Workspace
import com.kioschool.kioschoolapi.domain.workspace.entity.WorkspaceSetting
import com.kioschool.kioschoolapi.domain.workspace.service.WorkspaceService
import com.kioschool.kioschoolapi.factory.SampleEntity
import com.kioschool.kioschoolapi.global.common.entity.BaseEntity
import com.kioschool.kioschoolapi.global.og.facade.OgFacade
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
    val sut = OgFacade(workspaceService, ogService)

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
})
