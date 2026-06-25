package com.apix.memory.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;

/**
 * 会话实体 — 对标 MySQL: conversations 表
 */
@TableName("conversations")
public class Conversation {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_uid")
    private String userUid;

    @TableField("conversation_uid")
    private String conversationUid;

    private String title;

    @TableField("session_id")
    private String sessionId;

    @TableField("last_active_at")
    private LocalDateTime lastActiveAt;

    @TableField("latest_cursor")
    private Long latestCursor;

    @TableField("has_new_message")
    private Boolean hasNewMessage;

    @TableField("is_pinned")
    private Boolean isPinned;

    @TableField("is_deleted")
    private Boolean isDeleted;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("latest_timestamp")
    private Long latestTimestamp;

    @TableField("work_space")
    private String workSpace;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUserUid() { return userUid; }
    public void setUserUid(String userUid) { this.userUid = userUid; }

    public String getConversationUid() { return conversationUid; }
    public void setConversationUid(String conversationUid) { this.conversationUid = conversationUid; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public LocalDateTime getLastActiveAt() { return lastActiveAt; }
    public void setLastActiveAt(LocalDateTime lastActiveAt) { this.lastActiveAt = lastActiveAt; }

    public Long getLatestCursor() { return latestCursor; }
    public void setLatestCursor(Long latestCursor) { this.latestCursor = latestCursor; }

    public Boolean getHasNewMessage() { return hasNewMessage; }
    public void setHasNewMessage(Boolean hasNewMessage) { this.hasNewMessage = hasNewMessage; }

    public Boolean getPinned() { return isPinned; }
    public void setPinned(Boolean pinned) { isPinned = pinned; }

    public Boolean getDeleted() { return isDeleted; }
    public void setDeleted(Boolean deleted) { isDeleted = deleted; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public Long getLatestTimestamp() { return latestTimestamp; }
    public void setLatestTimestamp(Long latestTimestamp) { this.latestTimestamp = latestTimestamp; }

    public String getWorkSpace() { return workSpace; }
    public void setWorkSpace(String workSpace) { this.workSpace = workSpace; }
}
