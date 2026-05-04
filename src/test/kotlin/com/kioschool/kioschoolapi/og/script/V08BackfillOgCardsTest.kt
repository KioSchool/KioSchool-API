package com.kioschool.kioschoolapi.og.script

import com.kioschool.kioschoolapi.domain.workspace.entity.Workspace
import com.kioschool.kioschoolapi.domain.workspace.entity.WorkspaceImage
import com.kioschool.kioschoolapi.domain.workspace.entity.WorkspaceSetting
import com.kioschool.kioschoolapi.domain.workspace.repository.WorkspaceRepository
import com.kioschool.kioschoolapi.domain.workspace.service.OgCardGenerator
import com.kioschool.kioschoolapi.factory.SampleEntity
import com.kioschool.kioschoolapi.global.common.entity.BaseEntity
import com.kioschool.kioschoolapi.global.schedule.script.OgBackfillStep
import com.kioschool.kioschoolapi.global.schedule.script.OgBackfillStep.Result
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.Optional
import kotlin.reflect.full.superclasses

class V08BackfillOgCardsTest : DescribeSpec({
    val workspaceRepository = mockk<WorkspaceRepository>()
    val ogCardGenerator = mockk<OgCardGenerator>()
    val sut = OgBackfillStep(workspaceRepository, ogCardGenerator)

    beforeEach { clearMocks(workspaceRepository, ogCardGenerator) }

    fun BaseEntity.setBaseId(id: Long) {
        val f = this::class.superclasses.first().java.getDeclaredField("id")
        f.isAccessible = true
        f.set(this, id)
    }

    // Build a fresh Workspace per test to avoid cross-test contamination from
    // SampleEntity.workspace being a mutable singleton.
    fun newWorkspace(
        workspaceId: Long,
        ogImageUrl: String? = null,
        imagesWithIds: List<Pair<Long, String>> = emptyList(),
    ): Workspace {
        val ws = Workspace(
            name = "test",
            owner = SampleEntity.user,
            workspaceSetting = WorkspaceSetting(),
            ogImageUrl = ogImageUrl,
        )
        ws.setBaseId(workspaceId)
        imagesWithIds.forEach { (id, url) ->
            val img = WorkspaceImage(workspace = ws, url = url)
            img.setBaseId(id)
            ws.images.add(img)
        }
        return ws
    }

    describe("OgBackfillStep.processOne") {
        it("returns SKIPPED when ogImageUrl is already populated") {
            val ws = newWorkspace(
                workspaceId = 1L,
                ogImageUrl = "existing.png",
                imagesWithIds = listOf(1L to "https://cdn/photo.jpg"),
            )
            every { workspaceRepository.findById(1L) } returns Optional.of(ws)

            val result = sut.processOne(1L)

            assert(result == Result.SKIPPED)
            verify(exactly = 0) { ogCardGenerator.generate(any(), any()) }
            verify(exactly = 0) { workspaceRepository.save(any()) }
        }

        it("returns SKIPPED when workspace has no images") {
            val ws = newWorkspace(
                workspaceId = 2L,
                ogImageUrl = null,
                imagesWithIds = emptyList(),
            )
            every { workspaceRepository.findById(2L) } returns Optional.of(ws)

            val result = sut.processOne(2L)

            assert(result == Result.SKIPPED)
            verify(exactly = 0) { ogCardGenerator.generate(any(), any()) }
            verify(exactly = 0) { workspaceRepository.save(any()) }
        }

        it("returns SKIPPED when workspace not found") {
            every { workspaceRepository.findById(99L) } returns Optional.empty()

            val result = sut.processOne(99L)

            assert(result == Result.SKIPPED)
            verify(exactly = 0) { ogCardGenerator.generate(any(), any()) }
            verify(exactly = 0) { workspaceRepository.save(any()) }
        }

        it("generates and saves when ogImageUrl is null and a photo exists") {
            val ws = newWorkspace(
                workspaceId = 3L,
                ogImageUrl = null,
                imagesWithIds = listOf(1L to "photo.jpg"),
            )
            every { workspaceRepository.findById(3L) } returns Optional.of(ws)
            every { ogCardGenerator.generate(3L, "photo.jpg") } returns "https://og/new.png"
            every { workspaceRepository.save(ws) } returns ws

            val result = sut.processOne(3L)

            assert(result == Result.PROCESSED)
            assert(ws.ogImageUrl == "https://og/new.png")
            verify(exactly = 1) { ogCardGenerator.generate(3L, "photo.jpg") }
            verify(exactly = 1) { workspaceRepository.save(ws) }
        }
    }
})
