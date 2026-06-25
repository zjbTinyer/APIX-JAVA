package com.apix.agent.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 生成状态管理器 — 对标 Python: GenerationManager。
 *
 * 管理每次 AI 生成的生命周期（创建、更新缓存、中止）。
 * 每个 clientId 可以有多个 generation，但同一 historyId 只能有一个活跃 generation。
 */
@Component
public class GenerationManager {

    private static final Logger log = LoggerFactory.getLogger(GenerationManager.class);

    /** clientId -> (generationId -> GenerationState) */
    private final Map<String, Map<String, GenerationState>> connections = new ConcurrentHashMap<>();

    /** clientId -> 活跃 generationId 列表 */
    private final Map<String, String> activeGenerations = new ConcurrentHashMap<>();

    /**
     * 创建新的生成任务。
     * 自动中止同一 historyId 的旧生成。
     */
    public synchronized String createGeneration(String clientId, String historyId) {
        // 中止旧的
        abortByHistoryId(clientId, historyId);

        String genId = UUID.randomUUID().toString().replace("-", "");

        GenerationState state = new GenerationState();
        state.historyId = historyId;
        state.generationId = genId;
        state.clientId = clientId;
        state.status = "running";

        connections.computeIfAbsent(clientId, k -> new ConcurrentHashMap<>())
            .put(genId, state);

        activeGenerations.put(clientId + ":" + historyId, genId);
        log.info("[GenerationManager] Created generation: {} for client={}", genId, clientId);

        return genId;
    }

    /**
     * 获取生成状态。
     */
    public GenerationState getGeneration(String clientId, String generationId) {
        Map<String, GenerationState> gens = connections.get(clientId);
        return gens != null ? gens.get(generationId) : null;
    }

    /**
     * 中止指定 clientId 和 historyId 的生成。
     */
    public synchronized void abortByHistoryId(String clientId, String historyId) {
        String key = clientId + ":" + historyId;
        String oldGenId = activeGenerations.remove(key);
        if (oldGenId != null) {
            Map<String, GenerationState> gens = connections.get(clientId);
            if (gens != null) {
                GenerationState state = gens.get(oldGenId);
                if (state != null) {
                    state.status = "aborted";
                    log.warn("[GenerationManager] Aborted generation: {} for client={}", oldGenId, clientId);
                }
            }
        }
    }

    /**
     * 标记生成为完成。
     */
    public void finishGeneration(String clientId, String generationId) {
        GenerationState state = getGeneration(clientId, generationId);
        if (state != null) {
            state.status = "finished";
        }
    }

    /**
     * 生成状态内部类。
     */
    public static class GenerationState {
        public String historyId;
        public String generationId;
        public String clientId;
        public String status;   // running / finished / aborted

        public String content = "";
        public String think = "";
        public long createdAt = System.currentTimeMillis();
    }
}
