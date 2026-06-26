package com.apix.agent.core.graph.node;

import com.apix.agent.core.llm.LlmAdapter;
import com.apix.agent.core.tools.ToolRegistry;
import com.apix.common.model.StreamWriter;
import com.apix.common.model.ApixEventEnvelopeTarget;
import com.apix.common.exception.ConflictToolCallsException;
import com.apix.common.exception.InvalidOutputsException;
import com.apix.common.model.AgentConfig;
import com.apix.common.model.MainAgentState;
import com.apix.agent.core.graph.AgentGraph;
import com.apix.agent.core.graph.AgentNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * LLM 调用节点 — 对标 Python: AgentNodeBase.llm_call。
 *
 * 将组装好的消息列表发送给 LLM，
 * 处理流式响应和 tool_calls。
 * 使用真实的 LlmAdapter 进行 API 调用。
 */
public class LlmCallNode implements AgentNode {

    private static final Logger log = LoggerFactory.getLogger(LlmCallNode.class);

    private static ToolRegistry toolRegistry;

    public static void setToolRegistry(ToolRegistry registry) {
        toolRegistry = registry;
    }

    @Override
    public String getName() {
        return AgentGraph.NODE_LLM_CALL;
    }

    @Override
    @SuppressWarnings("unchecked")
    public String execute(MainAgentState state) {
        log.debug("[LlmCallNode] Calling LLM, calls={}", state.getLlmCalls());

        try {
            AgentConfig config = state.getConfig();
            if (config == null)
                throw new RuntimeException("AgentConfig is null");

            List<Map<String, Object>> messages = state.getMessages();
            LlmAdapter adapter = LlmAdapter.create(config);

            // 构建工具列表（function calling），将配置中的权限开关映射为 LLM 可用的工具
            List<Map<String, Object>> tools = buildToolsForLlm(state, config);

            // 判断是否支持流式推送（有 WebSocket 连接时）
            StreamWriter writer = state.getStreamWriter();
            boolean streaming = writer != null && !config.isPureChatOn();

            if (streaming) {
                // ====== 流式路径：逐 token 推送 think_chunk_rtn / content_chunk_rtn ======
                return executeStreaming(state, adapter, messages, config, writer, tools);
            } else {
                // ====== 非流式路径（HTTP 测试或纯聊天） ======
                return executeNonStreaming(state, adapter, messages, config, tools);
            }

        } catch (ConflictToolCallsException e) {
            log.warn("[LlmCallNode] Conflict tool calls: {}", e.getMessage());
            state.setError("conflict_tools");
            state.setErrorDetail(e.getMessage());
            return handleError(state);

        } catch (InvalidOutputsException e) {
            log.warn("[LlmCallNode] Invalid outputs: {}", e.getMessage());
            state.setError("others");
            state.setErrorDetail(e.getMessage());
            return handleError(state);

        } catch (Exception e) {
            log.error("[LlmCallNode] LLM call failed: {}", e.getMessage(), e);
            state.setError("others");
            state.setErrorDetail(e.getMessage());
            return handleError(state);
        }
    }

    /**
     * 流式执行 — SSE 逐 token 推送，实现打字机效果。
     */
    @SuppressWarnings("unchecked")
    private String executeStreaming(MainAgentState state, LlmAdapter adapter,
            List<Map<String, Object>> messages,
            AgentConfig config, StreamWriter writer,
            List<Map<String, Object>> tools) throws Exception {
        long startTime = System.currentTimeMillis();

        // 流式响应收集
        StringBuilder fullContent = new StringBuilder();
        StringBuilder fullReasoning = new StringBuilder();
        final Map<String, Object>[] finalResponse = new Map[1];
        final Throwable[] finalError = new Throwable[1];

        Object lock = new Object();
        boolean[] done = { false };

        // 发起流式调用（传入工具列表以实现 function calling）
        adapter.streamCall(messages, config, tools, new LlmAdapter.StreamCallback() {
            @Override
            public void onChunk(Map<String, Object> chunk) {
                ApixEventEnvelopeTarget target = state.getTarget();

                // 思考链 chunk → think_chunk_rtn
                Object reasoning = chunk.get("reasoning_content");
                if (reasoning != null) {
                    String text = reasoning.toString();
                    fullReasoning.append(text);
                    Map<String, Object> data = new LinkedHashMap<>();
                    data.put("event_name", "think_chunk_rtn");
                    data.put("content", text);
                    writer.sendEvent("think_chunk_rtn", target, data);
                }

                // 内容 chunk → content_chunk_rtn
                Object content = chunk.get("content");
                if (content != null) {
                    String text = content.toString();
                    fullContent.append(text);
                    Map<String, Object> data = new LinkedHashMap<>();
                    data.put("event_name", "content_chunk_rtn");
                    data.put("content", text);
                    writer.sendEvent("content_chunk_rtn", target, data);
                }
            }

            @Override
            public void onDone(Map<String, Object> response) {
                finalResponse[0] = response;
                synchronized (lock) {
                    done[0] = true;
                    lock.notify();
                }
            }

            @Override
            public void onError(Exception e) {
                finalError[0] = e;
                synchronized (lock) {
                    done[0] = true;
                    lock.notify();
                }
            }
        });

        // 等待流式完成
        synchronized (lock) {
            if (!done[0])
                lock.wait();
        }
        if (finalError[0] != null) {
            throw new RuntimeException("Stream error", finalError[0]);
        }

        long duration = System.currentTimeMillis() - startTime;
        log.info("[LlmCallNode] Streaming completed in {}ms ({} reasoning + {} content)",
                duration, fullReasoning.length(), fullContent.length());

        // 构造 AI 消息
        Map<String, Object> response = finalResponse[0];
        if (response == null)
            response = new LinkedHashMap<>();

        String content = fullContent.length() > 0 ? fullContent.toString()
                : (String) response.getOrDefault("content", "");
        String reasoning = fullReasoning.length() > 0 ? fullReasoning.toString()
                : (String) response.get("reasoning_content");

        List<Map<String, Object>> toolCalls = (List<Map<String, Object>>) response.get("tool_calls");

        Map<String, Object> aiMessage = new LinkedHashMap<>();
        aiMessage.put("role", "assistant");
        aiMessage.put("content", content);
        if (reasoning != null && !reasoning.isEmpty()) {
            aiMessage.put("reasoning_content", reasoning);
        }
        if (toolCalls != null && !toolCalls.isEmpty()) {
            aiMessage.put("tool_calls", toolCalls);
        }
        state.getMessages().add(aiMessage);
        state.setLlmCalls(state.getLlmCalls() + 1);

        // 路由判断
        if (toolCalls != null && !toolCalls.isEmpty()) {
            validateToolCalls(toolCalls);
            state.setCurrentToolCalls(normalizeToolCalls(toolCalls));
            log.info("[LlmCallNode] LLM requested {} tool calls", toolCalls.size());
            return AgentGraph.NODE_TOOLS;
        }

        if (content.isEmpty() && (reasoning == null || reasoning.isEmpty())) {
            throw new InvalidOutputsException("Empty AI message from LLM");
        }

        log.info("[LlmCallNode] Streaming text response: reasoning={}chars, content={}chars",
                reasoning != null ? reasoning.length() : 0, content.length());
        return AgentGraph.NODE_MESSAGES_PERSIST;
    }

    /**
     * 非流式执行（HTTP 测试或纯聊天模式）。
     */
    @SuppressWarnings("unchecked")
    private String executeNonStreaming(MainAgentState state, LlmAdapter adapter,
            List<Map<String, Object>> messages,
            AgentConfig config,
            List<Map<String, Object>> tools) {
        long startTime = System.currentTimeMillis();
        Map<String, Object> response = adapter.call(messages, config, tools);
        long duration = System.currentTimeMillis() - startTime;
        log.info("[LlmCallNode] LLM responded in {}ms", duration);

        String finishReason = (String) response.getOrDefault("finish_reason", "stop");
        List<Map<String, Object>> toolCalls = (List<Map<String, Object>>) response.get("tool_calls");

        Map<String, Object> aiMessage = new LinkedHashMap<>();
        aiMessage.put("role", "assistant");
        aiMessage.put("content", response.getOrDefault("content", ""));
        Object reasoning = response.get("reasoning_content");
        if (reasoning != null)
            aiMessage.put("reasoning_content", reasoning);
        if (toolCalls != null && !toolCalls.isEmpty())
            aiMessage.put("tool_calls", toolCalls);
        state.getMessages().add(aiMessage);
        state.setLlmCalls(state.getLlmCalls() + 1);

        if (toolCalls != null && !toolCalls.isEmpty()) {
            validateToolCalls(toolCalls);
            state.setCurrentToolCalls(normalizeToolCalls(toolCalls));
            log.info("[LlmCallNode] LLM requested {} tool calls", toolCalls.size());
            return AgentGraph.NODE_TOOLS;
        }

        String content = (String) response.getOrDefault("content", "");
        if (content.isEmpty() && reasoning == null) {
            throw new InvalidOutputsException("Empty AI message from LLM");
        }
        log.info("[LlmCallNode] LLM returned text response ({} chars)", content.length());
        return AgentGraph.NODE_MESSAGES_PERSIST;
    }

    /**
     * 为 LLM 构建 function calling 工具列表。
     */
    private List<Map<String, Object>> buildToolsForLlm(MainAgentState state, AgentConfig config) {
        if (config.isPureChatOn() || toolRegistry == null) {
            return Collections.emptyList();
        }

        // 根据配置收集权限
        List<String> permissions = new ArrayList<>();
        if (config.isEnableFileOperation())
            permissions.add("file_operation");
        if (config.isEnableWebSearch())
            permissions.add("web_search");
        if (config.isEnableKnowledgeRetrieval())
            permissions.add("knowledge_retrieval");
        if (config.isEnableCommandOperation())
            permissions.add("command_operation");
        if (config.isEnableSkillLoad())
            permissions.add("skill_load");
        if (config.isEnableAgentAssign())
            permissions.add("agent_assign");
        if (config.isEnableTaskFlow())
            permissions.add("task_flow");
        permissions.add("default"); // 始终包含默认工具

        String role = state.getAgentRole();
        boolean workspaceReady = config.getWorkDir() != null && !config.getWorkDir().isEmpty();

        return toolRegistry.getToolsForPermissions(permissions, role, workspaceReady);
    }

    /**
     * 工具调用冲突检测。
     */
    private void validateToolCalls(List<Map<String, Object>> toolCalls) {
        Set<String> conflictTools = ToolRegistry.CONFLICT_TOOL_SET;
        Set<String> seen = new LinkedHashSet<>();

        for (Map<String, Object> tc : toolCalls) {
            String name = extractToolName(tc);
            if (name == null || name.isEmpty()) {
                throw new ConflictToolCallsException("Empty tool name");
            }
            if (conflictTools.contains(name)) {
                if (!seen.add(name)) {
                    throw new ConflictToolCallsException(
                            "Conflict tool calls: " + name, seen);
                }
            }
        }
    }

    /**
     * 标准化 tool_calls 格式（处理 OpenAI / DeepSeek 不同返回格式）。
     */
    private List<Map<String, Object>> normalizeToolCalls(List<Map<String, Object>> rawCalls) {
        List<Map<String, Object>> result = new ArrayList<>();

        for (Map<String, Object> tc : rawCalls) {
            Map<String, Object> normalized = new LinkedHashMap<>();

            // id
            normalized.put("id", tc.getOrDefault("id", UUID.randomUUID().toString()));

            // name — 可能在 function.function.name 或者直接 name
            String name = extractToolName(tc);
            normalized.put("name", name);

            // arguments — 可能在 function.function.arguments 或者直接 arguments
            String argsStr = extractToolArgs(tc);
            normalized.put("args", parseArgs(argsStr));

            result.add(normalized);
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    private String extractToolName(Map<String, Object> tc) {
        // 直接 name
        String name = (String) tc.get("name");
        if (name != null)
            return name;

        // function.name
        Object func = tc.get("function");
        if (func instanceof Map) {
            name = (String) ((Map<String, Object>) func).get("name");
            if (name != null)
                return name;
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    private String extractToolArgs(Map<String, Object> tc) {
        // 直接 arguments
        Object args = tc.get("arguments");
        if (args instanceof String)
            return (String) args;
        if (args != null)
            return com.alibaba.fastjson.JSON.toJSONString(args);

        // function.arguments
        Object func = tc.get("function");
        if (func instanceof Map) {
            args = ((Map<String, Object>) func).get("arguments");
            if (args instanceof String)
                return (String) args;
            if (args != null)
                return com.alibaba.fastjson.JSON.toJSONString(args);
        }

        return "{}";
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseArgs(String argsStr) {
        try {
            if (argsStr == null || argsStr.isEmpty())
                return new LinkedHashMap<>();
            return com.alibaba.fastjson.JSON.parseObject(argsStr);
        } catch (Exception e) {
            return new LinkedHashMap<>();
        }
    }

    /**
     * 错误处理路由 — 对标 Python: route_after_llm。
     */
    private String handleError(MainAgentState state) {
        int retryCount = state.getLlmRetryCount();
        String errorType = state.getError();

        if (retryCount > 3) {
            log.warn("[LlmCallNode] Max retries exceeded");
            return AgentGraph.END;
        }

        state.setLlmRetryCount(retryCount + 1);

        if ("rate_limit".equals(errorType)) {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            return AgentGraph.NODE_LLM_CALL;
        }

        if ("token_exceed".equals(errorType)) {
            return AgentGraph.NODE_CONTEXT_SUMMARY;
        }

        return AgentGraph.NODE_LLM_CALL;
    }
}
