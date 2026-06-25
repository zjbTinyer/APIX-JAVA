package com.apix.agent.core.pipeline;

import com.apix.common.model.ApixEventEnvelope;
import com.apix.common.model.ApixEventEnvelopeTarget;
import com.apix.common.model.StreamWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Agent 流式事件写入器 — 对标 Python: AgentStreamWriter。
 *
 * 将 Agent 执行过程中的事件推送给前端。
 * 支持普通事件和阻塞事件（等待用户确认）。
 * 实现 StreamWriter 接口供 MainAgentState 引用（消除循环依赖）。
 */
public class AgentStreamWriter implements StreamWriter {

    private static final Logger log = LoggerFactory.getLogger(AgentStreamWriter.class);

    /** 阻塞事件 Future 缓存: targetHash -> (blockId -> CompletableFuture) */
    private static final Map<String, Map<String, CompletableFuture<Object>>> BLOCKING_FUTURES = new ConcurrentHashMap<>();

    private final String generationId;

    /** WebSocket 发送器，由外部注入 */
    private final WebSocketSender sender;

    public AgentStreamWriter(String generationId, WebSocketSender sender) {
        this.generationId = generationId;
        this.sender = sender;
    }

    /**
     * 发送普通事件。
     */
    public void sendEvent(String event, ApixEventEnvelopeTarget target,
                          Map<String, Object> data) {
        ApixEventEnvelope envelope = new ApixEventEnvelope(
            event, target, data, generationId, System.currentTimeMillis() / 1000.0
        );
        sender.send(target, envelope);
    }

    /**
     * 发送阻塞事件 — 等待前端返回确认。
     */
    public CompletableFuture<Object> sendBlockingEvent(String event, ApixEventEnvelopeTarget target,
                                                        Map<String, Object> data) {
        String blockId = java.util.UUID.randomUUID().toString().replace("-", "");
        CompletableFuture<Object> future = new CompletableFuture<>();

        String targetHash = targetHash(target);
        BLOCKING_FUTURES.computeIfAbsent(targetHash, k -> new ConcurrentHashMap<>())
            .put(blockId, future);

        data = data != null ? new ConcurrentHashMap<>(data) : new ConcurrentHashMap<>();
        data.put("blockId", blockId);

        sendEvent(event, target, data);
        log.warn("[AgentStreamWriter] Blocking event sent, target={}, blockId={}", targetHash, blockId);

        return future;
    }

    /**
     * 解析阻塞事件（由前端调用来回复）。
     */
    public static boolean resolveBlock(ApixEventEnvelopeTarget target, String blockId, Object result) {
        String targetHash = targetHash(target);
        Map<String, CompletableFuture<Object>> futures = BLOCKING_FUTURES.get(targetHash);
        if (futures == null) return false;

        CompletableFuture<Object> future = futures.remove(blockId);
        if (future == null) return false;

        future.complete(result);
        log.info("[AgentStreamWriter] Block resolved: target={}, blockId={}", targetHash, blockId);
        return true;
    }

    private static String targetHash(ApixEventEnvelopeTarget target) {
        return target.getId() + ":" + target.getPlatform() + ":" + target.getConversationId();
    }

    /**
     * WebSocket 发送器接口。
     */
    public interface WebSocketSender {
        void send(ApixEventEnvelopeTarget target, ApixEventEnvelope envelope);
    }
}
