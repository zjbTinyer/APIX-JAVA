package com.apix.memory.controller;

import com.apix.common.model.R;
import com.apix.memory.entity.Message;
import com.apix.memory.mapper.MessageMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 记忆服务 API — 对标 Python: MEMORY/memory_module/routers/memory_record.py
 */
@RestController
@RequestMapping("/memory/memory")
public class MemoryController {

    private static final Logger log = LoggerFactory.getLogger(MemoryController.class);

    @Autowired
    private MessageMapper messageMapper;

    @Autowired
    private com.apix.memory.mapper.ConversationMapper conversationMapper;

    /**
     * 追加消息 — 对标 Python: append_message
     *
     * Request body:
     * {
     *   "client_id": "...",
     *   "session_id": "",
     *   "history_id": "conversation_uid",
     *   "messages": {
     *     "role": "human|ai|system|tools",
     *     "content": "...",
     *     "generation_id": "...",
     *     "think": "..."
     *   }
     * }
     */
    @PostMapping("/append_message")
    public R<Map<String, Object>> appendMessage(@RequestBody Map<String, Object> payload) {
        log.info("[Memory] append_message");

        String userUid = (String) payload.get("client_id");
        String conversationUid = (String) payload.get("history_id");

        @SuppressWarnings("unchecked")
        Map<String, Object> msgData = (Map<String, Object>) payload.get("messages");
        if (msgData == null) {
            return R.fail("messages field is required");
        }

        try {
            // 查找 conversation_id
            com.apix.memory.entity.Conversation conv =
                conversationMapper.findByConversationUid(conversationUid);

            Message message = new Message();
            message.setUserUid(userUid != null ? userUid : "unknown");
            message.setConversationUid(conversationUid != null ? conversationUid : "unknown");
            if (conv != null) {
                message.setConversationId(conv.getId());
            } else {
                message.setConversationId(0L); // 临时值，后续会修复
            }
            message.setGenerationId((String) msgData.getOrDefault("generation_id", ""));
            message.setNodeId((String) msgData.getOrDefault("node_id",
                UUID.randomUUID().toString().substring(0, 16)));
            message.setParentId((String) msgData.getOrDefault("parent_id", "-"));
            message.setRole((String) msgData.getOrDefault("role", "human"));
            message.setContent((String) msgData.getOrDefault("content", ""));
            message.setThink((String) msgData.get("think"));

            // extra / info 转为 JSON 字符串
            Object extra = msgData.get("extra");
            if (extra != null) {
                message.setExtra(com.alibaba.fastjson.JSON.toJSONString(extra));
            }
            Object info = msgData.get("info");
            if (info != null) {
                message.setInfo(com.alibaba.fastjson.JSON.toJSONString(info));
            }

            message.setCreatedAt(LocalDateTime.now());
            message.setMsgTimestamp(System.currentTimeMillis() / 1000);
            message.setDeleted(false);

            // 获取下一个 cursor
            Long maxCursor = messageMapper.getMaxCursorByConversationUid(conversationUid);
            message.setMsgCursor(maxCursor != null ? maxCursor + 1 : 1);

            messageMapper.insert(message);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("message_id", message.getId());
            result.put("msg_cursor", message.getMsgCursor());

            log.info("[Memory] Message appended: id={}, cursor={}", message.getId(), message.getMsgCursor());
            return R.ok(result);

        } catch (Exception e) {
            log.error("[Memory] append_message failed", e);
            return R.error(500, "Failed to append message: " + e.getMessage());
        }
    }

    /**
     * 获取历史消息列表。
     *
     * Request body:
     * {
     *   "history_id": "conversation_uid"
     * }
     */
    @PostMapping("/get_messages")
    public R<List<Map<String, Object>>> getMessages(@RequestBody Map<String, Object> payload) {
        String conversationUid = (String) payload.get("history_id");
        log.info("[Memory] get_messages: conversation={}", conversationUid);

        try {
            List<Message> messages = messageMapper.findByConversationUid(conversationUid);

            List<Map<String, Object>> result = messages.stream().map(msg -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", msg.getId());
                m.put("role", msg.getRole());
                m.put("content", msg.getContent());
                m.put("think", msg.getThink());
                m.put("generation_id", msg.getGenerationId());
                m.put("node_id", msg.getNodeId());
                m.put("parent_id", msg.getParentId());
                m.put("msg_cursor", msg.getMsgCursor());
                m.put("created_at", msg.getCreatedAt() != null ? msg.getCreatedAt().toString() : "");
                return m;
            }).collect(Collectors.toList());

            return R.ok(result);

        } catch (Exception e) {
            log.error("[Memory] get_messages failed", e);
            return R.error(500, "Failed to get messages: " + e.getMessage());
        }
    }
}
