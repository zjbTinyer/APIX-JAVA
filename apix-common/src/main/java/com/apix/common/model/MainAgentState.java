package com.apix.common.model;

import java.util.List;
import java.util.Map;

/**
 * 主 Agent 图的核心状态 — 对标 Python: MainAgentState。
 * 在 LangGraph 图执行过程中流转的上下文对象。
 */
public class MainAgentState extends GraphRuntimeContext {

    /** 用户输入 */
    private Map<String, Object> input;

    /** 是否重新生成 */
    private boolean reGenerate;

    /** 对话消息列表 */
    private List<Map<String, Object>> messages;

    /** 流式事件写入器 — 由 AgentGraph.execute() 注入，节点通过它推流式事件 */
    private transient StreamWriter streamWriter;

    /** 当前工具调用 */
    private List<Map<String, Object>> currentToolCalls;

    /** 长期记忆（跨会话） */
    private String longtermMemory;

    /** 短期摘要 */
    private String shorttermMemory;

    /** 规则提示 */
    private String rulePrompt;

    /** 运行时提示（待办、工作区提示等） */
    private String runtimePrompt;

    /** LLM 调用次数 */
    private int llmCalls;

    /** LLM 重试次数 */
    private int llmRetryCount;

    /** 错误类型 */
    private String error;

    /** 错误详情 */
    private String errorDetail;

    /**
     * 上下文压缩级别：
     * 0 = 不压缩
     * 1 = 丢弃工具消息内容
     * 2 = LLM 摘要压缩
     */
    private int contextCompressLevel;

    /** Docker 容器 ID */
    private String sandbox;

    /** 技能列表 */
    private List<Map<String, Object>> skills;

    /** 加载的技能缓存： (name, injected, content) */
    private List<SkillCache> loadedSkillsCache;

    /** 文档列表 */
    private List<Map<String, Object>> documents;

    // ==================== Getter / Setter ====================

    public Map<String, Object> getInput() {
        return input;
    }

    public void setInput(Map<String, Object> input) {
        this.input = input;
    }

    public boolean isReGenerate() {
        return reGenerate;
    }

    public void setReGenerate(boolean reGenerate) {
        this.reGenerate = reGenerate;
    }

    public List<Map<String, Object>> getMessages() {
        return messages;
    }

    public void setMessages(List<Map<String, Object>> messages) {
        this.messages = messages;
    }

    public List<Map<String, Object>> getCurrentToolCalls() {
        return currentToolCalls;
    }

    public void setCurrentToolCalls(List<Map<String, Object>> currentToolCalls) {
        this.currentToolCalls = currentToolCalls;
    }

    public String getLongtermMemory() {
        return longtermMemory;
    }

    public void setLongtermMemory(String longtermMemory) {
        this.longtermMemory = longtermMemory;
    }

    public String getShorttermMemory() {
        return shorttermMemory;
    }

    public void setShorttermMemory(String shorttermMemory) {
        this.shorttermMemory = shorttermMemory;
    }

    public String getRulePrompt() {
        return rulePrompt;
    }

    public void setRulePrompt(String rulePrompt) {
        this.rulePrompt = rulePrompt;
    }

    public String getRuntimePrompt() {
        return runtimePrompt;
    }

    public void setRuntimePrompt(String runtimePrompt) {
        this.runtimePrompt = runtimePrompt;
    }

    public int getLlmCalls() {
        return llmCalls;
    }

    public void setLlmCalls(int llmCalls) {
        this.llmCalls = llmCalls;
    }

    public int getLlmRetryCount() {
        return llmRetryCount;
    }

    public void setLlmRetryCount(int llmRetryCount) {
        this.llmRetryCount = llmRetryCount;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getErrorDetail() {
        return errorDetail;
    }

    public void setErrorDetail(String errorDetail) {
        this.errorDetail = errorDetail;
    }

    public int getContextCompressLevel() {
        return contextCompressLevel;
    }

    public void setContextCompressLevel(int contextCompressLevel) {
        this.contextCompressLevel = contextCompressLevel;
    }

    public String getSandbox() {
        return sandbox;
    }

    public void setSandbox(String sandbox) {
        this.sandbox = sandbox;
    }

    public List<Map<String, Object>> getSkills() {
        return skills;
    }

    public void setSkills(List<Map<String, Object>> skills) {
        this.skills = skills;
    }

    public List<SkillCache> getLoadedSkillsCache() {
        return loadedSkillsCache;
    }

    public void setLoadedSkillsCache(List<SkillCache> loadedSkillsCache) {
        this.loadedSkillsCache = loadedSkillsCache;
    }

    public List<Map<String, Object>> getDocuments() {
        return documents;
    }

    public void setDocuments(List<Map<String, Object>> documents) {
        this.documents = documents;
    }

    public StreamWriter getStreamWriter() {
        return streamWriter;
    }

    public void setStreamWriter(StreamWriter streamWriter) {
        this.streamWriter = streamWriter;
    }

    /** 技能缓存内部类 */
    public static class SkillCache {
        private String name;
        private boolean injected;
        private String content;

        public SkillCache() {
        }

        public SkillCache(String name, boolean injected, String content) {
            this.name = name;
            this.injected = injected;
            this.content = content;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public boolean isInjected() {
            return injected;
        }

        public void setInjected(boolean injected) {
            this.injected = injected;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }
}
