package com.apix.common.model;

/**
 * Agent 运行时上下文基类 — 对标 Python: GraphRuntimeContext。
 */
public class GraphRuntimeContext {

    private String agentName;
    private String agentRole; // AgentRole 的 value
    private String clientId;
    private String sessionId;
    private String historyId;
    private String platform; // 平台标识
    private ApixEventEnvelopeTarget target;
    private String generationId;
    private String nodeId;
    private String parentNodeId;
    private AgentConfig config;
    private long timestamp;

    public String getAgentName() {
        return agentName;
    }

    public void setAgentName(String agentName) {
        this.agentName = agentName;
    }

    public String getAgentRole() {
        return agentRole;
    }

    public void setAgentRole(String agentRole) {
        this.agentRole = agentRole;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getHistoryId() {
        return historyId;
    }

    public void setHistoryId(String historyId) {
        this.historyId = historyId;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public ApixEventEnvelopeTarget getTarget() {
        return target;
    }

    public void setTarget(ApixEventEnvelopeTarget target) {
        this.target = target;
    }

    public String getGenerationId() {
        return generationId;
    }

    public void setGenerationId(String generationId) {
        this.generationId = generationId;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public String getParentNodeId() {
        return parentNodeId;
    }

    public void setParentNodeId(String parentNodeId) {
        this.parentNodeId = parentNodeId;
    }

    public AgentConfig getConfig() {
        return config;
    }

    public void setConfig(AgentConfig config) {
        this.config = config;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
