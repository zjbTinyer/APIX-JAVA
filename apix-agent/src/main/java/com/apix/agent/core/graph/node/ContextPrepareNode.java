package com.apix.agent.core.graph.node;

import com.alibaba.fastjson.JSON;
import com.apix.agent.client.MemoryClient;
import com.apix.common.model.AgentConfig;
import com.apix.common.model.MainAgentState;
import com.apix.agent.core.graph.AgentGraph;
import com.apix.agent.core.graph.AgentNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 上下文准备节点 — 对标 Python: AgentNodeBase.context_prepare。
 *
 * 从 Memory 服务拉取对话历史、短期/长期记忆，
 * 构造 LLM 输入的消息列表（系统提示 + 历史消息 + 用户输入）。
 */
public class ContextPrepareNode implements AgentNode {

    private static final Logger log = LoggerFactory.getLogger(ContextPrepareNode.class);

    /** 节点缓存的 MemoryClient（通过静态方式注入） */
    private static MemoryClient memoryClient;

    public static void setMemoryClient(MemoryClient client) {
        memoryClient = client;
    }

    @Override
    public String getName() {
        return AgentGraph.NODE_CONTEXT_PREPARE;
    }

    @Override
    @SuppressWarnings("unchecked")
    public String execute(MainAgentState state) {
        log.debug("[ContextPrepareNode] Preparing context for generation={}", state.getGenerationId());

        List<Map<String, Object>> messages = new ArrayList<>();
        AgentConfig config = state.getConfig();

        // 1. 构造系统提示词
        String systemPrompt = buildSystemPrompt(state);
        Map<String, Object> sysMsg = new LinkedHashMap<>();
        sysMsg.put("role", "system");
        sysMsg.put("content", systemPrompt);
        messages.add(sysMsg);

        // 2. 加载短期记忆
        if (config != null && config.isEnableShorttermMemory() && memoryClient != null) {
            String shortTerm = memoryClient.getShorttermMemory(
                state.getClientId(), state.getHistoryId());
            if (shortTerm != null && !shortTerm.isEmpty()) {
                Map<String, Object> memMsg = new LinkedHashMap<>();
                memMsg.put("role", "system");
                memMsg.put("content", "会话摘要: " + shortTerm);
                messages.add(memMsg);
            }
        }

        // 3. 加载长期记忆
        if (config != null && config.isEnableLongtermMemory() && memoryClient != null) {
            String longTerm = memoryClient.getLongtermMemory(state.getClientId());
            if (longTerm != null && !longTerm.isEmpty()) {
                Map<String, Object> memMsg = new LinkedHashMap<>();
                memMsg.put("role", "system");
                memMsg.put("content", "长期记忆: " + longTerm);
                messages.add(memMsg);
            }
        }

        // 4. 加载历史消息
        if (memoryClient != null) {
            List<Map<String, Object>> history = memoryClient.getMessages(state.getHistoryId());
            for (Map<String, Object> msg : history) {
                String role = (String) msg.getOrDefault("role", "user");
                // 跳过 system / info 消息
                if ("system".equals(role) || "info".equals(role)) continue;

                Map<String, Object> converted = new LinkedHashMap<>();
                converted.put("role", role);
                converted.put("content", msg.getOrDefault("content", ""));
                Object think = msg.get("think");
                if (think != null) converted.put("reasoning_content", think);
                messages.add(converted);
            }
        }

        // 5. 添加用户当前输入
        Map<String, Object> input = state.getInput();
        if (input != null) {
            String inputContent = (String) input.getOrDefault("content", "");
            if (!inputContent.isEmpty()) {
                Map<String, Object> userMsg = new LinkedHashMap<>();
                userMsg.put("role", "user");
                userMsg.put("content", inputContent);
                messages.add(userMsg);
            }
        }

        state.setMessages(messages);
        log.info("[ContextPrepareNode] Prepared {} messages for LLM", messages.size());

        return AgentGraph.NODE_CONTEXT_SUMMARY;
    }

    /**
     * 构造系统提示词。
     */
    private String buildSystemPrompt(MainAgentState state) {
        StringBuilder sb = new StringBuilder();
        AgentConfig config = state.getConfig();

        // 时间信息
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        sb.append("Current time: ").append(sdf.format(new Date())).append("\n\n");

        // 角色卡
        if (config != null && config.getRolePrompt() != null
            && config.getRolePrompt().getDefinition() != null
            && !config.getRolePrompt().getDefinition().isEmpty()) {
            sb.append("你是一个").append(config.getRolePrompt().getName())
              .append("，").append(config.getRolePrompt().getDefinition()).append("\n\n");
        } else {
            sb.append("你是一个有用的 AI 助手。\n\n");
        }

        // 工具列表
        sb.append("可用工具:\n");
        // 工具列表将在 LLM 调用时通过 function calling 传入

        // 运行时提示
        if (state.getRuntimePrompt() != null && !state.getRuntimePrompt().isEmpty()) {
            sb.append("\n").append(state.getRuntimePrompt()).append("\n");
        }

        return sb.toString();
    }
}
