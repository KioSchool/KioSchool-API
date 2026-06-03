package com.kioschool.kioschoolapi.global.sse

import com.kioschool.kioschoolapi.global.websocket.dto.Message
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

@Service
class SseEmitterService {
    private val emitters = ConcurrentHashMap<Long, CopyOnWriteArrayList<SseEmitter>>()

    fun addEmitter(workspaceId: Long, emitter: SseEmitter) {
        emitters.getOrPut(workspaceId) { CopyOnWriteArrayList() }.add(emitter)
    }

    fun removeEmitter(workspaceId: Long, emitter: SseEmitter) {
        emitters[workspaceId]?.remove(emitter)
    }

    fun sendToWorkspace(workspaceId: Long, message: Message) {
        val list = emitters[workspaceId] ?: return
        val event = SseEmitter.event().data(message, MediaType.APPLICATION_JSON)
        val dead = mutableListOf<SseEmitter>()
        list.forEach { emitter ->
            try {
                emitter.send(event)
            } catch (e: IOException) {
                dead.add(emitter)
            }
        }
        dead.forEach { list.remove(it) }
    }

    fun emitterCount(workspaceId: Long): Int = emitters[workspaceId]?.size ?: 0
}
