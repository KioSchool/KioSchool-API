package com.kioschool.kioschoolapi.og.service

import com.kioschool.kioschoolapi.domain.workspace.entity.Workspace
import com.kioschool.kioschoolapi.domain.workspace.entity.WorkspaceSetting
import com.kioschool.kioschoolapi.domain.workspace.service.WorkspaceService
import com.kioschool.kioschoolapi.factory.SampleEntity
import com.kioschool.kioschoolapi.global.common.entity.BaseEntity
import com.kioschool.kioschoolapi.global.og.service.OgCardGenerator
import com.kioschool.kioschoolapi.global.og.service.OgService
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import kotlin.reflect.full.superclasses

class OgServiceTest : DescribeSpec({
    val workspaceService = mockk<WorkspaceService>()
    val ogCardGenerator = mockk<OgCardGenerator>()
    val sut = OgService(
        workspaceService = workspaceService,
        ogCardGenerator = ogCardGenerator,
        fallbackImageUrl = "https://kio-school.com/preview.png",
    )

    beforeEach { clearMocks(workspaceService, ogCardGenerator) }

    fun BaseEntity.setBaseId(id: Long) {
        val f = this::class.superclasses.first().java.getDeclaredField("id")
        f.isAccessible = true
        f.set(this, id)
    }

    fun newWorkspace(
        workspaceId: Long = 100L,
        name: String = "test",
        ogImageUrl: String? = null,
    ): Workspace {
        val ws = Workspace(
            name = name,
            owner = SampleEntity.user,
            workspaceSetting = WorkspaceSetting(),
            ogImageUrl = ogImageUrl,
        )
        ws.setBaseId(workspaceId)
        return ws
    }

    describe("renderOrderHtml") {
        it("renders og:image=ogImageUrl and og:title with workspace name when found") {
            val ws = newWorkspace(
                workspaceId = 1L,
                name = "테스트주점",
                ogImageUrl = "https://og/test.png",
            )
            every { workspaceService.findWorkspaceOrNull(ws.id) } returns ws

            val body = sut.renderOrderHtml(ws.id)

            assert(body.contains("""<meta property="og:image"""))
            assert(body.contains("https://og/test.png"))
            assert(body.contains("테스트주점 · 키오스쿨"))
        }

        it("falls back to global preview image when workspaceId is null") {
            val body = sut.renderOrderHtml(null)

            assert(body.contains("https://kio-school.com/preview.png"))
            assert(!body.contains("og:image\" content=\"\""))
        }

        it("falls back when workspaceId does not match any workspace") {
            every { workspaceService.findWorkspaceOrNull(999L) } returns null

            val body = sut.renderOrderHtml(999L)

            assert(body.contains("https://kio-school.com/preview.png"))
        }

        it("falls back when workspace exists but has no ogImageUrl yet") {
            val ws = newWorkspace(workspaceId = 2L, ogImageUrl = null)
            every { workspaceService.findWorkspaceOrNull(ws.id) } returns ws

            val body = sut.renderOrderHtml(ws.id)

            assert(body.contains("https://kio-school.com/preview.png"))
        }

        it("escapes HTML special characters in workspace name to prevent meta injection") {
            val ws = newWorkspace(
                workspaceId = 3L,
                name = """Tap"House <script>""",
                ogImageUrl = "https://og/x.png",
            )
            every { workspaceService.findWorkspaceOrNull(ws.id) } returns ws

            val body = sut.renderOrderHtml(ws.id)

            assert(!body.contains("<script>"))
            assert(body.contains("&lt;script&gt;") && body.contains("&quot;"))
        }

        it("includes a canonical og:url with the workspaceId query string") {
            val ws = newWorkspace(workspaceId = 4L, ogImageUrl = "https://og/x.png")
            every { workspaceService.findWorkspaceOrNull(ws.id) } returns ws

            val body = sut.renderOrderHtml(ws.id)

            assert(body.contains("https://kio-school.com/order?workspaceId=${ws.id}"))
        }
    }

    describe("predictedOgUrl + regenerateOgCard") {
        it("delegates to OgCardGenerator") {
            every { ogCardGenerator.predictedUrl(7L, "src.jpg") } returns "https://og/predicted.png"
            every { ogCardGenerator.generate(7L, "src.jpg") } returns "https://og/generated.png"

            assert(sut.predictedOgUrl(7L, "src.jpg") == "https://og/predicted.png")
            assert(sut.regenerateOgCard(7L, "src.jpg") == "https://og/generated.png")
        }
    }
})
