package com.apix.agent.core;

import com.apix.common.model.AgentConfig;
import com.apix.common.model.AgentRole;
import com.apix.common.model.MainAgentState;
import com.apix.agent.core.graph.AgentGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Agent 创建器 — 对标 Python: AgentCreator。
 *
 * 职责：
 * 1. 根据配置动态创建 Agent 图
 * 2. 缓存编译好的图（避免重复创建）
 * 3. 管理图的生命周期
 */
@Component
public class AgentCreator {

    private static final Logger log = LoggerFactory.getLogger(AgentCreator.class);

    /** 图缓存: hashKey -> GraphCacheEntry */
    private final Map<Integer, GraphCacheEntry> graphCache = new ConcurrentHashMap<>();

    /**
     * 创建主 Agent 图。
     */
    public AgentGraph createAgent(String agentName, String agentRole, AgentConfig config) {
        log.info("[AgentCreator] Creating agent: name={}, role={}", agentName, agentRole);

        // 计算缓存 key
        int hashKey = buildHashKey(agentName, agentRole, config);

        // 尝试从缓存获取
        GraphCacheEntry cached = graphCache.get(hashKey);
        if (cached != null && !cached.isExpired()) {
            log.info("[AgentCreator] Cache hit for agent: {}", agentName);
            cached.refreshTtl();
            return cached.graph;
        }

        // 创建新图
        AgentGraph graph = new AgentGraph();
        // TODO: 根据权限配置工具列表

        // 缓存
        graphCache.put(hashKey, new GraphCacheEntry(graph));
        log.info("[AgentCreator] Created new agent graph: {}", agentName);

        return graph;
    }

    /**
     * 创建子 Agent 图。
     */
    public AgentGraph createSubAgent(String agentName, String agentRole, AgentConfig config) {
        log.info("[AgentCreator] Creating sub-agent: name={}, role={}", agentName, agentRole);
        // 子 Agent 与主 Agent 使用相同图结构，但状态类型不同
        return createAgent(agentName, agentRole, config);
    }

    /**
     * 标记图不再使用。
     */
    public void done(AgentGraph graph) {
        // 图缓存会自动 TTL 过期，无需主动清理
        // 此处可做资源释放
    }

    private int buildHashKey(String agentName, String agentRole, AgentConfig config) {
        return (agentName + agentRole + config.toString()).hashCode();
    }

    /**
     * 图缓存条目。
     */
    private static class GraphCacheEntry {
        private final AgentGraph graph;
        private long expireAt;

        GraphCacheEntry(AgentGraph graph) {
            this.graph = graph;
            this.expireAt = System.currentTimeMillis() + 600_000; // 10 min TTL
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expireAt;
        }

        void refreshTtl() {
            this.expireAt = System.currentTimeMillis() + 600_000;
        }
    }
}
