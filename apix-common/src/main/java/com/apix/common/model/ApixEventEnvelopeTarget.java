package com.apix.common.model;

/**
 * 事件投递目标 — 消息要发给谁。
 * 对标 Python: ApixEventEnvelopeTarget
 */
public class ApixEventEnvelopeTarget {

    private String id; // client_id
    private String platform; // 平台标识（如 "default"）
    private String conversationId; // history_id

    public ApixEventEnvelopeTarget() {
    }

    public ApixEventEnvelopeTarget(String id, String platform, String conversationId) {
        this.id = id;
        this.platform = platform;
        this.conversationId = conversationId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }
}
