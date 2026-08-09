package com.sis.iids.collab;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * R-15 SSE 推送注册表（D2 选型 A：SSE 单向推送，轻量够 P1 用）。
 * 按方案维护订阅者；评论/变更/在线状态事件即时推送给在线协作者。
 */
@Component
public class CollabEventBus {

    private static final Logger log = LoggerFactory.getLogger(CollabEventBus.class);
    private static final long SSE_TIMEOUT_MS = 30 * 60 * 1000L;

    private final Map<Long, List<SseEmitter>> emittersByScenario = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Long scenarioId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        emittersByScenario.computeIfAbsent(scenarioId, k -> new CopyOnWriteArrayList<>()).add(emitter);
        emitter.onCompletion(() -> remove(scenarioId, emitter));
        emitter.onTimeout(() -> remove(scenarioId, emitter));
        emitter.onError(e -> remove(scenarioId, emitter));
        try {
            emitter.send(SseEmitter.event().name("connected").data("ok"));
        } catch (IOException ex) {
            remove(scenarioId, emitter);
        }
        return emitter;
    }

    /** 向方案的全部订阅者广播事件。 */
    public void publish(Long scenarioId, String eventName, Object payload) {
        List<SseEmitter> emitters = emittersByScenario.get(scenarioId);
        if (emitters == null) {
            return;
        }
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(payload));
            } catch (IOException | IllegalStateException ex) {
                log.debug("SSE 推送失败，移除订阅: scenarioId={}, event={}", scenarioId, eventName);
                remove(scenarioId, emitter);
            }
        }
    }

    private void remove(Long scenarioId, SseEmitter emitter) {
        List<SseEmitter> emitters = emittersByScenario.get(scenarioId);
        if (emitters != null) {
            emitters.remove(emitter);
            if (emitters.isEmpty()) {
                emittersByScenario.remove(scenarioId);
            }
        }
    }
}
