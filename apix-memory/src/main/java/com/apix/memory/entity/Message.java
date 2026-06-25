package com.apix.memory.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;

/**
 * 消息实体 — 对标 MySQL: messages 表
 *
 * 支持树形结构 (node_id, parent_id)，实现消息分支编辑。
 */
@TableName("messages")
public class Message {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_uid")
    private String userUid;

    @TableField("conversation_id")
    private Long conversationId;

    @TableField("conversation_uid")
    private String conversationUid;

    @TableField("generation_id")
    private String generationId;

    @TableField("node_id")
    private String nodeId;

    @TableField("parent_id")
    private String parentId;

    private String role;      // human / ai / system / tools / info

    private String content;

    private String think;

    private String extra;     // JSON

    private String info;      // JSON

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("msg_cursor")
    private Long msgCursor;

    @TableField("msg_timestamp")
    private Long msgTimestamp;

    @TableField("is_deleted")
    private Boolean isDeleted;

    // ==================== Getter / Setter ====================

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUserUid() { return userUid; }
    public void setUserUid(String userUid) { this.userUid = userUid; }

    public Long getConversationId() { return conversationId; }
    public void setConversationId(Long conversationId) { this.conversationId = conversationId; }

    public String getConversationUid() { return conversationUid; }
    public void setConversationUid(String conversationUid) { this.conversationUid = conversationUid; }

    public String getGenerationId() { return generationId; }
    public void setGenerationId(String generationId) { this.generationId = generationId; }

    public String getNodeId() { return nodeId; }
    public void setNodeId(String nodeId) { this.nodeId = nodeId; }

    public String getParentId() { return parentId; }
    public void setParentId(String parentId) { this.parentId = parentId; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getThink() { return think; }
    public void setThink(String think) { this.think = think; }

    public String getExtra() { return extra; }
    public void setExtra(String extra) { this.extra = extra; }

    public String getInfo() { return info; }
    public void setInfo(String info) { this.info = info; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public Long getMsgCursor() { return msgCursor; }
    public void setMsgCursor(Long msgCursor) { this.msgCursor = msgCursor; }

    public Long getMsgTimestamp() { return msgTimestamp; }
    public void setMsgTimestamp(Long msgTimestamp) { this.msgTimestamp = msgTimestamp; }

    public Boolean getDeleted() { return isDeleted; }
    public void setDeleted(Boolean deleted) { isDeleted = deleted; }
}
