package com.kioschool.kioschoolapi.workspace.listener

import com.kioschool.kioschoolapi.domain.workspace.entity.Workspace
import com.kioschool.kioschoolapi.domain.workspace.entity.WorkspaceImage
import com.kioschool.kioschoolapi.domain.workspace.entity.WorkspaceSetting
import com.kioschool.kioschoolapi.domain.workspace.event.WorkspaceUpdatedEvent
import com.kioschool.kioschoolapi.domain.workspace.listener.WorkspaceOgImageListener
import com.kioschool.kioschoolapi.domain.workspace.repository.WorkspaceRepository
import com.kioschool.kioschoolapi.global.og.service.OgService
import com.kioschool.kioschoolapi.factory.SampleEntity
import com.kioschool.kioschoolapi.global.common.entity.BaseEntity
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.util.Optional
import kotlin.reflect.full.superclasses

class WorkspaceOgImageListenerTest : DescribeSpec({
    val workspaceRepository = mockk<WorkspaceRepository>()
    val ogService = mockk<OgService>()
    val sut = WorkspaceOgImageListener(workspaceRepository, ogService)

    beforeEach { clearMocks(workspaceRepository, ogService) }

    fun BaseEntity.setBaseId(id: Long) {
        val f = this::class.superclasses.first().java.getDeclaredField("id")
        f.isAccessible = true
        f.set(this, id)
    }

    // Build a fresh Workspace per test to avoid cross-test contamination from
    // SampleEntity.workspace being a mutable singleton.
    fun newWorkspace(
        workspaceId: Long = 100L,
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

    describe("on(WorkspaceUpdatedEvent)") {
        it("regenerates and saves ogImageUrl when the primary photo's expected url differs") {
            val ws = newWorkspace(
                workspaceId = 1L,
                ogImageUrl = "https://cdn/og/old.png",
                imagesWithIds = listOf(1L to "https://cdn/photo.jpg"),
            )
            every { workspaceRepository.findById(1L) } returns Optional.of(ws)
            every { ogService.predictedOgUrl(1L, "https://cdn/photo.jpg") } returns "https://cdn/og/new.png"
            every { ogService.regenerateOgCard(1L, "https://cdn/photo.jpg") } returns "https://cdn/og/new.png"
            val saved = slot<Workspace>()
            every { workspaceRepository.save(capture(saved)) } answers { firstArg() }

            sut.on(WorkspaceUpdatedEvent(1L))

            assert(saved.captured.ogImageUrl == "https://cdn/og/new.png")
            verify(exactly = 1) { ogService.regenerateOgCard(1L, "https://cdn/photo.jpg") }
            verify(exactly = 1) { workspaceRepository.save(any()) }
        }

        it("skips generate and save when ogImageUrl already matches expected hash (no photo change)") {
            val ws = newWorkspace(
                workspaceId = 2L,
                ogImageUrl = "https://cdn/og/same.png",
                imagesWithIds = listOf(1L to "https://cdn/photo.jpg"),
            )
            every { workspaceRepository.findById(2L) } returns Optional.of(ws)
            every { ogService.predictedOgUrl(2L, "https://cdn/photo.jpg") } returns "https://cdn/og/same.png"

            sut.on(WorkspaceUpdatedEvent(2L))

            verify(exactly = 0) { ogService.regenerateOgCard(any(), any()) }
            verify(exactly = 0) { workspaceRepository.save(any()) }
        }

        it("clears ogImageUrl to null when the workspace has no images and a previous og card existed") {
            val ws = newWorkspace(
                workspaceId = 3L,
                ogImageUrl = "https://cdn/og/old.png",
                imagesWithIds = emptyList(),
            )
            every { workspaceRepository.findById(3L) } returns Optional.of(ws)
            val saved = slot<Workspace>()
            every { workspaceRepository.save(capture(saved)) } answers { firstArg() }

            sut.on(WorkspaceUpdatedEvent(3L))

            assert(saved.captured.ogImageUrl == null)
            verify(exactly = 0) { ogService.regenerateOgCard(any(), any()) }
            verify(exactly = 1) { workspaceRepository.save(any()) }
        }

        it("preserves existing ogImageUrl when generation throws (Sentry-style soft failure)") {
            val ws = newWorkspace(
                workspaceId = 4L,
                ogImageUrl = "https://cdn/og/old.png",
                imagesWithIds = listOf(1L to "https://cdn/photo.jpg"),
            )
            every { workspaceRepository.findById(4L) } returns Optional.of(ws)
            every { ogService.predictedOgUrl(4L, "https://cdn/photo.jpg") } returns "https://cdn/og/new.png"
            every { ogService.regenerateOgCard(4L, "https://cdn/photo.jpg") } throws RuntimeException("S3 down")

            sut.on(WorkspaceUpdatedEvent(4L))

            assert(ws.ogImageUrl == "https://cdn/og/old.png")
            verify(exactly = 0) { workspaceRepository.save(any()) }
        }

        it("uses the image with the smallest id as the primary photo") {
            // Add images out of insertion order so minByOrNull(id) is the meaningful selector.
            val ws = newWorkspace(
                workspaceId = 5L,
                ogImageUrl = null,
                imagesWithIds = listOf(
                    7L to "https://cdn/seven.jpg",
                    3L to "https://cdn/three.jpg",
                    9L to "https://cdn/nine.jpg",
                ),
            )
            every { workspaceRepository.findById(5L) } returns Optional.of(ws)
            every { ogService.predictedOgUrl(5L, "https://cdn/three.jpg") } returns "https://cdn/og/three-card.png"
            every { ogService.regenerateOgCard(5L, "https://cdn/three.jpg") } returns "https://cdn/og/three-card.png"
            every { workspaceRepository.save(any()) } answers { firstArg() }

            sut.on(WorkspaceUpdatedEvent(5L))

            verify(exactly = 1) { ogService.predictedOgUrl(5L, "https://cdn/three.jpg") }
            verify(exactly = 1) { ogService.regenerateOgCard(5L, "https://cdn/three.jpg") }
            verify(exactly = 0) { ogService.regenerateOgCard(5L, "https://cdn/seven.jpg") }
            verify(exactly = 0) { ogService.regenerateOgCard(5L, "https://cdn/nine.jpg") }
        }

        it("does nothing when the workspace cannot be found") {
            every { workspaceRepository.findById(404L) } returns Optional.empty()

            sut.on(WorkspaceUpdatedEvent(404L))

            verify(exactly = 0) { ogService.regenerateOgCard(any(), any()) }
            verify(exactly = 0) { ogService.predictedOgUrl(any(), any()) }
            verify(exactly = 0) { workspaceRepository.save(any()) }
        }
    }
})
