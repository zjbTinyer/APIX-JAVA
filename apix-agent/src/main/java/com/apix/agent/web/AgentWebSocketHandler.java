package com.apix.agent.web;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.apix.common.model.ApixEventEnvelope;
import com.apix.common.model.ApixEventEnvelopeTarget;
import com.apix.agent.core.AgentRuntime;
import com.apix.agent.core.pipeline.AgentStreamWriter;
import com.apix.agent.core.pipeline.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket 处理器 — 对标 Python: routers/websocket.py
 *
 * 接收前端消息，转发到 EventHandler；
 * 同时将后端 Agent 事件翻译为前端理解的格式再推送。
 */
@Component
public class AgentWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(AgentWebSocketHandler.class);

    /** 已连接的客户端: clientId -> WebSocketSession */
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    @Autowired
    private EventHandler eventHandler;

    @Autowired
    private AgentRuntime agentRuntime;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String path = session.getUri() != null ? session.getUri().getPath() : "";
        String[] parts = path.split("/");
        String clientId = parts.length >= 4 ? parts[3] : session.getId();

        sessions.put(clientId, session);
        log.info("[WS] Client connected: {} ({})", clientId, session.getId());
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        String raw = message.getPayload();
        log.info("[WS] Received: {}", raw);

        try {
            JSONObject json = JSON.parseObject(raw);
            String action = json.getString("action");
            Map<String, Object> payload = json.getInnerMap();

            // sender 回调 — 后端 Agent 事件 → 前端格式 → 推送
            AgentStreamWriter.WebSocketSender sender = this::sendTranslatedToClient;

            switch (action) {
                case "chat_with_llm":
                    eventHandler.handleChatWithLlm(payload, sender);
                    break;
                case "abort_generation":
                    eventHandler.handleAbortGeneration(payload);
                    break;
                case "resolve_block":
                    eventHandler.handleResolveBlock(payload);
                    break;
                default:
                    log.warn("[WS] Unknown action: {}", action);
            }
        } catch (Exception e) {
            log.error("[WS] Failed to handle message: {}", e.getMessage(), e);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        // 清理断开客户端的待处理事件
        String disconnectedId = null;
        for (Map.Entry<String, WebSocketSession> entry : sessions.entrySet()) {
            if (entry.getValue().equals(session)) {
                disconnectedId = entry.getKey();
                break;
            }
        }
        if (disconnectedId != null) {
            sessions.remove(disconnectedId);
        }
        log.info("[WS] Client disconnected: {} (status={})", session.getId(), status);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("[WS] Transport error: {}", exception.getMessage());
        sessions.values().remove(session);
    }

    // =================================================================
    //  事件翻译：后端 ApixEventEnvelope → 前端 Vue 事件格式
    // =================================================================

    /**
     * 后端事件 → 前端格式 → 推送
     *
     * 前端期望格式:
     * {
     *   "generation_id": "...",
     *   "data": {
     *     "messages": { "event_name": "content_chunk_rtn", "content": "...", ... },
     *     "history_id": "..."
     *   }
     * }
     *
     * 前端事件名: msg_stream_start, think_chunk_rtn, content_chunk_rtn,
     *             msg_stream_end, msg_stream_abort, tool_exec_chunk_rtn
     */
    private void sendTranslatedToClient(ApixEventEnvelopeTarget target, ApixEventEnvelope envelope) {
        if (target == null) return;

        String backendEvent = envelope.getEvent();
        String generationId = envelope.getGenerationId();
        Map<String, Object> backendData = envelope.getData();

        // 后端事件 → 前端事件映射
        String frontendEvent = mapBackendToFrontendEvent(backendEvent);

        // 跳过无需转发的后端内部事件
        if (frontendEvent == null) return;

        // 构造前端格式
        Map<String, Object> frontendPayload = new LinkedHashMap<>();
        frontendPayload.put("generation_id", generationId);

        Map<String, Object> dataWrapper = new LinkedHashMap<>();
        dataWrapper.put("history_id", target.getConversationId());

        Map<String, Object> messages = new LinkedHashMap<>();
        messages.put("event_name", frontendEvent);

        // 提取 data.content 放到 messages.content
        if (backendData != null) {
            Object content = backendData.get("content");
            if (content != null) {
                messages.put("content", content);
            }
            // 额外字段透传
            Object node = backendData.get("node");
            if (node != null) messages.put("node", node);
            Object nodeIdData = backendData.get("node_id");
            if (nodeIdData != null) messages.put("node_id", nodeIdData);
        }

        dataWrapper.put("messages", messages);
        frontendPayload.put("data", dataWrapper);

        // 如果是 ai_message_return（最终回复），拆成三个事件模拟流式
        if ("ai_message_return".equals(backendEvent)) {
            sendRawToClient(target, frontendPayload);  // 先发 msg_stream_end

            // 补发最终 content
            Map<String, Object> endPayload = deepCopy(frontendPayload);
            getMessages(endPayload).put("event_name", "content_chunk_rtn");
            if (backendData != null) {
                getMessages(endPayload).put("content", backendData.get("content"));
            }
            sendRawToClient(target, endPayload);
        }

        sendRawToClient(target, frontendPayload);
    }

    /**
     * 后端事件名 → 前端事件名映射。
     * 返回 null 表示跳过该事件。
     */
    private String mapBackendToFrontendEvent(String backendEvent) {
        if (backendEvent == null) return null;
        switch (backendEvent) {
            case "llm_stream_start":
            case "essential_info_return":
                return "msg_stream_start";
            case "ai_message_return":
                return "msg_stream_end";
            case "llm_stream_end":
                return "msg_stream_end";
            case "llm_stream_error":
                return "msg_stream_abort";
            case "tool_exec_start":
                return "tool_exec_chunk_rtn";
            case "runtime_warning":
                return "token_limit_warning";
            case "error_occurred":
                return "msg_stream_abort";
            default:
                return null;
        }
    }

    /**
     * 直接发送 JSON 给客户端。
     */
    private void sendRawToClient(ApixEventEnvelopeTarget target, Map<String, Object> frontendPayload) {
        WebSocketSession session = sessions.get(target.getId());
        if (session == null || !session.isOpen()) {
            log.warn("[WS] Client not connected: {}", target.getId());
            return;
        }
        try {
            String json = JSON.toJSONString(frontendPayload);
            session.sendMessage(new TextMessage(json));
        } catch (IOException e) {
            log.error("[WS] Failed to send to {}: {}", target.getId(), e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getMessages(Map<String, Object> payload) {
        Map<String, Object> data = (Map<String, Object>) payload.get("data");
        if (data == null) return new LinkedHashMap<>();
        return (Map<String, Object>) data.getOrDefault("messages", new LinkedHashMap<>());
    }

    private Map<String, Object> deepCopy(Map<String, Object> original) {
        return JSON.parseObject(JSON.toJSONString(original));
    }

    /**
     * 获取已连接的客户端数量。
     */
    public int getConnectedClientCount() {
        return sessions.size();
    }
}
