package com.apix.agent.core;

import com.apix.agent.core.pipeline.AgentStreamWriter;
import com.apix.common.model.AgentConfig;
import com.apix.common.model.MainAgentState;
import com.apix.agent.core.graph.AgentGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Agent 运行时管理器 — 对标 Python: AgentRuningtime。
 *
 * 管理 Agent 生命周期、子 Agent 调度、后台任务。
 */
@Component
public class AgentRuntime {

    private static final Logger log = LoggerFactory.getLogger(AgentRuntime.class);

    @Autowired
    private AgentCreator agentCreator;

    @Autowired
    private GenerationManager generationManager;

    /** 线程池 */
    private ExecutorService executor;

    /** 子 Agent 任务队列 */
    private final BlockingQueue<SubAgentTask> taskQueue = new LinkedBlockingQueue<>();

    /** 子 Agent 停止请求队列 */
    private final BlockingQueue<String> stopRequestQueue = new LinkedBlockingQueue<>();

    /** 运行中的任务 */
    private final ConcurrentHashMap<String, Future<?>> runningTasks = new ConcurrentHashMap<>();

    /** 任务状态存储: (historyId, taskId) -> SubAgentState */
    private final ConcurrentHashMap<String, Map<String, Object>> taskStateStore = new ConcurrentHashMap<>();

    /** 主 Agent 执行中的 Future: generationId -> Future */
    private final ConcurrentHashMap<String, Future<?>> agentExecutionFutures = new ConcurrentHashMap<>();

    private final AtomicBoolean running = new AtomicBoolean(false);

    @PostConstruct
    public void start() {
        if (running.compareAndSet(false, true)) {
            executor = Executors.newCachedThreadPool();

            executor.submit(this::subAgentWorkerLoop);
            executor.submit(this::stopRequestLoop);

            log.info("[AgentRuntime] Started background workers");
        }
    }

    @PreDestroy
    public void stop() {
        if (running.compareAndSet(true, false)) {
            if (executor != null) {
                executor.shutdownNow();
            }
            log.info("[AgentRuntime] Stopped");
        }
    }

    // ==================== 主 Agent 异步执行 ====================

    /**
     * 异步执行主 Agent 图，通过 writer 流式推送事件。
     *
     * @param state        初始状态
     * @param writer       事件写入器
     * @param generationId 生成 ID（用于中止跟踪）
     */
    public void executeAgentAsync(MainAgentState state, AgentStreamWriter writer,
                                   String generationId) {
        AgentGraph graph = agentCreator.createAgent(
            state.getAgentName(), state.getAgentRole(), state.getConfig());

        // 异步提交到线程池
        Future<?> future = executor.submit(() -> {
            try {
                log.info("[AgentRuntime] Starting async agent execution: genId={}", generationId);

                // 检查是否被中止
                GenerationManager.GenerationState genState =
                    generationManager.getGeneration(state.getClientId(), generationId);
                if (genState == null || "aborted".equals(genState.status)) {
                    log.warn("[AgentRuntime] Generation aborted before execution: {}", generationId);
                    return;
                }

                // 执行图
                graph.execute(state, writer);

                // 标记完成
                generationManager.finishGeneration(state.getClientId(), generationId);
                log.info("[AgentRuntime] Async agent execution completed: genId={}", generationId);

            } catch (Exception e) {
                log.error("[AgentRuntime] Async agent execution failed: genId={}", generationId, e);
                generationManager.abortByHistoryId(state.getClientId(), state.getHistoryId());
            } finally {
                agentExecutionFutures.remove(generationId);
                agentCreator.done(graph);
            }
        });

        agentExecutionFutures.put(generationId, future);
    }

    /**
     * 提交主 Agent 任务（同步，返回图对象）。
     */
    public AgentGraph submitAgentTask(String agentRole, String agentName, AgentConfig config) {
        log.info("[AgentRuntime] Submit agent task: name={}, role={}", agentName, agentRole);
        return agentCreator.createAgent(agentName, agentRole, config);
    }

    /**
     * 中止指定 generationId 的 Agent 执行。
     */
    public void abortAgentExecution(String generationId) {
        Future<?> future = agentExecutionFutures.remove(generationId);
        if (future != null && !future.isDone()) {
            future.cancel(true);
            log.info("[AgentRuntime] Aborted agent execution: {}", generationId);
        }
    }

    // ==================== 子 Agent 调度 ====================

    /**
     * 提交子 Agent 任务。
     */
    public String submitSubAgentTask(Map<String, Object> initialState, AgentConfig config, String agentName) {
        String taskId = UUID.randomUUID().toString().replace("-", "");
        initialState.put("taskId", taskId);

        String historyId = (String) initialState.get("historyId");
        taskStateStore.put(historyId + ":" + taskId, initialState);

        taskQueue.offer(new SubAgentTask(agentName, initialState, config));
        log.info("[AgentRuntime] Submitted sub-agent task: {}", taskId);

        return taskId;
    }

    /**
     * 查询子 Agent 任务状态。
     */
    public Map<String, Object> queryTask(String historyId, String taskId) {
        return taskStateStore.get(historyId + ":" + taskId);
    }

    /**
     * 停止子 Agent 任务。
     */
    public void stopTask(String taskId) {
        stopRequestQueue.offer(taskId);
    }

    // ==================== 后台循环 ====================

    private void subAgentWorkerLoop() {
        log.info("[SubAgentWorker] Started");
        while (running.get()) {
            try {
                SubAgentTask task = taskQueue.poll(1, TimeUnit.SECONDS);
                if (task == null) continue;

                String taskId = (String) task.initialState.get("taskId");
                String historyId = (String) task.initialState.get("historyId");

                // 更新状态为 in_progress
                updateTaskState(historyId, taskId, "status", "in_progress");

                // 创建子 Agent
                AgentGraph graph = agentCreator.createSubAgent(
                    task.agentName,
                    (String) task.initialState.get("agentRole"),
                    task.config
                );

                // 执行
                // TODO: 实际的子 Agent 图执行逻辑
                log.info("[SubAgentWorker] Executing task: {}", taskId);

                updateTaskState(historyId, taskId, "status", "completed");

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("[SubAgentWorker] Error", e);
            }
        }
        log.info("[SubAgentWorker] Stopped");
    }

    private void stopRequestLoop() {
        log.info("[StopRequestLoop] Started");
        while (running.get()) {
            try {
                String taskId = stopRequestQueue.poll(1, TimeUnit.SECONDS);
                if (taskId == null) continue;

                Future<?> future = runningTasks.get(taskId);
                if (future != null && !future.isDone()) {
                    future.cancel(true);
                    log.info("[StopRequestLoop] Cancelled task: {}", taskId);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        log.info("[StopRequestLoop] Stopped");
    }

    private void updateTaskState(String historyId, String taskId, String key, Object value) {
        String storeKey = historyId + ":" + taskId;
        taskStateStore.computeIfAbsent(storeKey, k -> new ConcurrentHashMap<>()).put(key, value);
    }

    // ==================== 内部类 ====================

    private static class SubAgentTask {
        final String agentName;
        final Map<String, Object> initialState;
        final AgentConfig config;

        SubAgentTask(String agentName, Map<String, Object> initialState, AgentConfig config) {
            this.agentName = agentName;
            this.initialState = initialState;
            this.config = config;
        }
    }
}
