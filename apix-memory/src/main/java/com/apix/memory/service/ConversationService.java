package com.apix.memory.service;

import com.apix.memory.entity.Conversation;
import com.apix.memory.mapper.ConversationMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 会话服务。
 */
@Service
public class ConversationService {

    private static final Logger log = LoggerFactory.getLogger(ConversationService.class);

    @Autowired
    private ConversationMapper conversationMapper;

    /**
     * 获取用户的所有活跃会话。
     */
    public List<Conversation> getActiveConversations(String userUid) {
        return conversationMapper.findActiveByUserUid(userUid);
    }

    /**
     * 创建新会话。
     */
    public Conversation createConversation(String userUid, String title) {
        Conversation conv = new Conversation();
        conv.setUserUid(userUid);
        conv.setConversationUid(UUID.randomUUID().toString().replace("-", ""));
        conv.setTitle(title != null ? title : "新的聊天...");
        conv.setLastActiveAt(LocalDateTime.now());
        conv.setLatestCursor(0L);
        conv.setHasNewMessage(false);
        conv.setPinned(false);
        conv.setDeleted(false);
        conv.setLatestTimestamp(System.currentTimeMillis() / 1000);

        conversationMapper.insert(conv);
        log.info("[ConversationService] Created conversation: {} for user={}", conv.getConversationUid(), userUid);

        return conv;
    }

    /**
     * 软删除会话。
     */
    public boolean deleteConversation(String conversationUid) {
        conversationMapper.softDelete(conversationUid);
        return true;
    }
}
