package com.apix.common.model;

/**
 * Agent 配置 — 对标 Python: AgentConfigSchema。
 * 一个 Agent 的所有运行参数。
 */
public class AgentConfig {

    // ==================== 用户/会话信息 ====================
    private String clientId;
    private String sessionId;
    private String historyId;
    private String platform;

    // ==================== LLM 运行时 ====================
    private String modelsProvider; // openai / deepseek / ollama / custom-*
    private String modelName;
    private String apiKey;
    private double modelTemperature = 1.0;
    private String customProviderId;
    private boolean enableThink;
    private int llmCallsWarningThreshold;
    private boolean useModelVision; // 是否将图片发送给 LLM

    // ==================== Agent 行为 ====================
    private String workDir;
    private boolean keepToolsMessage; // 异步返回是否存库
    private boolean pureChatOn; // 纯聊天模式（无工具）

    // ==================== 记忆策略 ====================
    private boolean enableLongtermMemory;
    private boolean enableShorttermMemory; // true=LLM压缩, false=直接截断
    private int summaryTriggerThreshold; // 0=不压缩
    private int summaryExemptTailLength;

    // ==================== 能力开关（工具权限） ====================
    private boolean enableFileOperation;
    private boolean enableWebSearch;
    private boolean enableKnowledgeRetrieval;
    private boolean enableCommandOperation;
    private boolean enableSkillLoad;
    private boolean enableTaskFlow;
    private boolean enableAgentAssign;
    private boolean enableAgentSwarm;

    // ==================== 外部服务 ====================
    private String linkProvider;
    private String linkApiKey;
    private String contentProvider;
    private String contentApiKey;
    private String embedModel;
    private String webCleanerMode;
    private boolean autoSaveConfig;

    // ==================== 角色提示 ====================
    private RoleSchema rolePrompt;
    private boolean higherRolePromptPermission; // 是否将角色卡插入系统提示

    // ==================== Getter / Setter ====================

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

    public String getModelsProvider() {
        return modelsProvider;
    }

    public void setModelsProvider(String modelsProvider) {
        this.modelsProvider = modelsProvider;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public double getModelTemperature() {
        return modelTemperature;
    }

    public void setModelTemperature(double modelTemperature) {
        this.modelTemperature = modelTemperature;
    }

    public String getCustomProviderId() {
        return customProviderId;
    }

    public void setCustomProviderId(String customProviderId) {
        this.customProviderId = customProviderId;
    }

    public boolean isEnableThink() {
        return enableThink;
    }

    public void setEnableThink(boolean enableThink) {
        this.enableThink = enableThink;
    }

    public int getLlmCallsWarningThreshold() {
        return llmCallsWarningThreshold;
    }

    public void setLlmCallsWarningThreshold(int llmCallsWarningThreshold) {
        this.llmCallsWarningThreshold = llmCallsWarningThreshold;
    }

    public boolean isUseModelVision() {
        return useModelVision;
    }

    public void setUseModelVision(boolean useModelVision) {
        this.useModelVision = useModelVision;
    }

    public String getWorkDir() {
        return workDir;
    }

    public void setWorkDir(String workDir) {
        this.workDir = workDir;
    }

    public boolean isKeepToolsMessage() {
        return keepToolsMessage;
    }

    public void setKeepToolsMessage(boolean keepToolsMessage) {
        this.keepToolsMessage = keepToolsMessage;
    }

    public boolean isPureChatOn() {
        return pureChatOn;
    }

    public void setPureChatOn(boolean pureChatOn) {
        this.pureChatOn = pureChatOn;
    }

    public boolean isEnableLongtermMemory() {
        return enableLongtermMemory;
    }

    public void setEnableLongtermMemory(boolean enableLongtermMemory) {
        this.enableLongtermMemory = enableLongtermMemory;
    }

    public boolean isEnableShorttermMemory() {
        return enableShorttermMemory;
    }

    public void setEnableShorttermMemory(boolean enableShorttermMemory) {
        this.enableShorttermMemory = enableShorttermMemory;
    }

    public int getSummaryTriggerThreshold() {
        return summaryTriggerThreshold;
    }

    public void setSummaryTriggerThreshold(int summaryTriggerThreshold) {
        this.summaryTriggerThreshold = summaryTriggerThreshold;
    }

    public int getSummaryExemptTailLength() {
        return summaryExemptTailLength;
    }

    public void setSummaryExemptTailLength(int summaryExemptTailLength) {
        this.summaryExemptTailLength = summaryExemptTailLength;
    }

    public boolean isEnableFileOperation() {
        return enableFileOperation;
    }

    public void setEnableFileOperation(boolean enableFileOperation) {
        this.enableFileOperation = enableFileOperation;
    }

    public boolean isEnableWebSearch() {
        return enableWebSearch;
    }

    public void setEnableWebSearch(boolean enableWebSearch) {
        this.enableWebSearch = enableWebSearch;
    }

    public boolean isEnableKnowledgeRetrieval() {
        return enableKnowledgeRetrieval;
    }

    public void setEnableKnowledgeRetrieval(boolean enableKnowledgeRetrieval) {
        this.enableKnowledgeRetrieval = enableKnowledgeRetrieval;
    }

    public boolean isEnableCommandOperation() {
        return enableCommandOperation;
    }

    public void setEnableCommandOperation(boolean enableCommandOperation) {
        this.enableCommandOperation = enableCommandOperation;
    }

    public boolean isEnableSkillLoad() {
        return enableSkillLoad;
    }

    public void setEnableSkillLoad(boolean enableSkillLoad) {
        this.enableSkillLoad = enableSkillLoad;
    }

    public boolean isEnableTaskFlow() {
        return enableTaskFlow;
    }

    public void setEnableTaskFlow(boolean enableTaskFlow) {
        this.enableTaskFlow = enableTaskFlow;
    }

    public boolean isEnableAgentAssign() {
        return enableAgentAssign;
    }

    public void setEnableAgentAssign(boolean enableAgentAssign) {
        this.enableAgentAssign = enableAgentAssign;
    }

    public boolean isEnableAgentSwarm() {
        return enableAgentSwarm;
    }

    public void setEnableAgentSwarm(boolean enableAgentSwarm) {
        this.enableAgentSwarm = enableAgentSwarm;
    }

    public String getLinkProvider() {
        return linkProvider;
    }

    public void setLinkProvider(String linkProvider) {
        this.linkProvider = linkProvider;
    }

    public String getLinkApiKey() {
        return linkApiKey;
    }

    public void setLinkApiKey(String linkApiKey) {
        this.linkApiKey = linkApiKey;
    }

    public String getContentProvider() {
        return contentProvider;
    }

    public void setContentProvider(String contentProvider) {
        this.contentProvider = contentProvider;
    }

    public String getContentApiKey() {
        return contentApiKey;
    }

    public void setContentApiKey(String contentApiKey) {
        this.contentApiKey = contentApiKey;
    }

    public String getEmbedModel() {
        return embedModel;
    }

    public void setEmbedModel(String embedModel) {
        this.embedModel = embedModel;
    }

    public String getWebCleanerMode() {
        return webCleanerMode;
    }

    public void setWebCleanerMode(String webCleanerMode) {
        this.webCleanerMode = webCleanerMode;
    }

    public boolean isAutoSaveConfig() {
        return autoSaveConfig;
    }

    public void setAutoSaveConfig(boolean autoSaveConfig) {
        this.autoSaveConfig = autoSaveConfig;
    }

    public RoleSchema getRolePrompt() {
        return rolePrompt;
    }

    public void setRolePrompt(RoleSchema rolePrompt) {
        this.rolePrompt = rolePrompt;
    }

    public boolean isHigherRolePromptPermission() {
        return higherRolePromptPermission;
    }

    public void setHigherRolePromptPermission(boolean higherRolePromptPermission) {
        this.higherRolePromptPermission = higherRolePromptPermission;
    }
}
