package com.apix.agent.core.graph.node;

import com.apix.common.model.AgentConfig;
import com.apix.common.model.MainAgentState;
import com.apix.agent.core.graph.AgentGraph;
import com.apix.agent.core.graph.AgentNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * 上下文摘要/压缩节点 — 对标 Python: AgentNodeBase.context_summary。
 *
 * 当上下文超长时，自动触发压缩或截断：
 * - level 0: 不压缩
 * - level 1: 丢弃工具消息内容
 * - level 2: 调用 LLM 进行摘要压缩
 */
public class ContextSummaryNode implements AgentNode {

    private static final Logger log = LoggerFactory.getLogger(ContextSummaryNode.class);

    @Override
    public String getName() {
        return AgentGraph.NODE_CONTEXT_SUMMARY;
    }

    @Override
    @SuppressWarnings("unchecked")
    public String execute(MainAgentState state) {
        AgentConfig config = state.getConfig();
        List<Map<String, Object>> messages = state.getMessages();

        // 判断是否需要压缩
        int threshold = config != null ? config.getSummaryTriggerThreshold() : 0;
        int totalMessages = messages.size();

        if (threshold <= 0 || totalMessages <= threshold) {
            // 不需要压缩
            return AgentGraph.NODE_LLM_CALL;
        }

        // 计算压缩级别
        int level = determineCompressLevel(totalMessages, threshold);
        state.setContextCompressLevel(level);

        log.info("[ContextSummaryNode] Compressing: {} msgs, threshold={}, level={}",
            totalMessages, threshold, level);

        switch (level) {
            case 1:
                dropToolMessages(state);
                break;
            case 2:
                truncateMessages(state);
                break;
            default:
                break;
        }

        return AgentGraph.NODE_LLM_CALL;
    }

    /**
     * 根据消息数量决定压缩级别。
     */
    private int determineCompressLevel(int total, int threshold) {
        if (total > threshold * 3) return 2;   // 超过阈值 3 倍 → 截断
        if (total > threshold) return 1;       // 超过阈值 → 丢弃工具消息
        return 0;
    }

    /**
     * Level 1: 丢弃工具消息内容，只保留元数据。
     */
    @SuppressWarnings("unchecked")
    private void dropToolMessages(MainAgentState state) {
        List<Map<String, Object>> messages = state.getMessages();

        for (Map<String, Object> msg : messages) {
            if ("tool".equals(msg.get("role"))) {
                String content = (String) msg.get("content");
                if (content != null && content.length() > 200) {
                    msg.put("content", content.substring(0, 200) + "... (truncated by context compression)");
                }
            }
        }

        log.info("[ContextSummaryNode] Dropped tool message contents (kept metadata)");
    }

    /**
     * Level 2: 截断最旧的消息，保留尾部 N 条。
     */
    @SuppressWarnings("unchecked")
    private void truncateMessages(MainAgentState state) {
        List<Map<String, Object>> messages = state.getMessages();
        AgentConfig config = state.getConfig();

        int exemptTail = config != null ? config.getSummaryExemptTailLength() : 64;
        if (exemptTail <= 0) exemptTail = 64;

        // 始终保留系统提示
        int startIndex = 0;
        if (!messages.isEmpty() && "system".equals(messages.get(0).get("role"))) {
            startIndex = 1;
        }

        // 保留最后 exemptTail 条
        int keepFrom = Math.max(startIndex, messages.size() - exemptTail);
        if (keepFrom > startIndex) {
            List<Map<String, Object>> keptMessages = new ArrayList<>();
            // 系统提示
            if (startIndex > 0) keptMessages.add(messages.get(0));
            // 尾部消息
            keptMessages.addAll(messages.subList(keepFrom, messages.size()));

            state.setMessages(keptMessages);
            log.info("[ContextSummaryNode] Truncated from {} to {} messages",
                messages.size(), keptMessages.size());
        }
    }
}
