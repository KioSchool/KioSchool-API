package com.kioschool.kioschoolapi.global.sse

import com.kioschool.kioschoolapi.global.websocket.dto.Message
import org.springframework.http.MediaType
import org.springframework.scheduling.annotation.Scheduled
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

    /**
     * 열려있는 모든 SSE 스트림에 주기적으로 keep-alive 코멘트를 보낸다.
     * 앞단 프록시/로드밸런서가 idle 스트림을 끊어 EventSource가 재연결 폭풍을 일으키는 것을 방지하고,
     * 동시에 이미 끊긴(broken/completed) emitter를 정리해 메모리 누수도 막는다.
     * 코멘트(`:`) 라인이라 클라이언트는 무시한다.
     */
    @Scheduled(fixedRate = 20000)
    fun sendHeartbeat() {
        emitters.forEach { (_, list) ->
            val dead = mutableListOf<SseEmitter>()
            list.forEach { emitter ->
                try {
                    emitter.send(SseEmitter.event().comment("heartbeat"))
                } catch (e: Exception) {
                    dead.add(emitter)
                }
            }
            dead.forEach { list.remove(it) }
        }
    }
}
