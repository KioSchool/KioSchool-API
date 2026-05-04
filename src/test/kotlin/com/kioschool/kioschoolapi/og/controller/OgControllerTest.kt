package com.kioschool.kioschoolapi.og.controller

import com.kioschool.kioschoolapi.domain.workspace.entity.Workspace
import com.kioschool.kioschoolapi.domain.workspace.entity.WorkspaceSetting
import com.kioschool.kioschoolapi.domain.workspace.repository.WorkspaceRepository
import com.kioschool.kioschoolapi.factory.SampleEntity
import com.kioschool.kioschoolapi.global.common.entity.BaseEntity
import com.kioschool.kioschoolapi.global.og.controller.OgController
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import java.util.Optional
import kotlin.reflect.full.superclasses

class OgControllerTest : DescribeSpec({
    val workspaceRepository = mockk<WorkspaceRepository>()
    val sut = OgController(
        workspaceRepository = workspaceRepository,
        fallbackImageUrl = "https://kio-school.com/preview.png",
    )

    beforeEach { clearMocks(workspaceRepository) }

    fun BaseEntity.setBaseId(id: Long) {
        val f = this::class.superclasses.first().java.getDeclaredField("id")
        f.isAccessible = true
        f.set(this, id)
    }

    // Build a fresh Workspace per test to avoid cross-test contamination from
    // SampleEntity.workspace being a mutable singleton.
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

    describe("ogOrder") {
        it("renders og:image=ogImageUrl and og:title with workspace name when found") {
            val ws = newWorkspace(
                workspaceId = 1L,
                name = "테스트주점",
                ogImageUrl = "https://og/test.png",
            )
            every { workspaceRepository.findById(ws.id) } returns Optional.of(ws)

            val response = sut.ogOrder(ws.id)
            val body = response.body!!

            assert(response.statusCode.value() == 200)
            assert(body.contains("""<meta property="og:image"""))
            assert(body.contains("https://og/test.png"))
            assert(body.contains("테스트주점"))
            val cacheControl = response.headers.cacheControl ?: ""
            assert(cacheControl.contains("max-age=600"))
            assert(cacheControl.contains("public"))
        }

        it("falls back to global preview image when workspaceId is null") {
            val response = sut.ogOrder(null)
            val body = response.body!!

            assert(body.contains("https://kio-school.com/preview.png"))
            assert(!body.contains("og:image\" content=\"\""))
        }

        it("falls back when workspaceId does not match any workspace") {
            every { workspaceRepository.findById(999L) } returns Optional.empty()

            val response = sut.ogOrder(999L)
            val body = response.body!!

            assert(body.contains("https://kio-school.com/preview.png"))
            assert(response.statusCode.value() == 200)
        }

        it("falls back when workspace exists but has no ogImageUrl yet") {
            val ws = newWorkspace(workspaceId = 2L, ogImageUrl = null)
            every { workspaceRepository.findById(ws.id) } returns Optional.of(ws)

            val response = sut.ogOrder(ws.id)

            assert(response.body!!.contains("https://kio-school.com/preview.png"))
        }

        it("escapes HTML special characters in workspace name to prevent meta injection") {
            val ws = newWorkspace(
                workspaceId = 3L,
                name = """Tap"House <script>""",
                ogImageUrl = "https://og/x.png",
            )
            every { workspaceRepository.findById(ws.id) } returns Optional.of(ws)

            val body = sut.ogOrder(ws.id).body!!

            assert(!body.contains("<script>"))
            assert(body.contains("&lt;script&gt;") || body.contains("&quot;"))
        }

        it("includes a canonical og:url with the workspaceId query string") {
            val ws = newWorkspace(workspaceId = 4L, ogImageUrl = "https://og/x.png")
            every { workspaceRepository.findById(ws.id) } returns Optional.of(ws)

            val body = sut.ogOrder(ws.id).body!!

            assert(body.contains("https://kio-school.com/order?workspaceId=${ws.id}"))
        }
    }
})
