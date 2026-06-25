package com.apix.agent.core.tools.impl;

import com.apix.agent.core.AgentRuntime;
import com.apix.common.model.AgentConfig;
import com.apix.common.model.MainAgentState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 子 Agent 分配工具 — 对标 Python: tools/assistant/call_assistant.py :: assign_sub_assistant
 *
 * 主 Agent 调用此工具将子任务分配给子 Agent。
 */
public class SubAgentAssignTool extends BaseTool {

    private static final Logger log = LoggerFactory.getLogger(SubAgentAssignTool.class);

    private final AgentRuntime agentRuntime;

    public SubAgentAssignTool(AgentRuntime agentRuntime) {
        this.agentRuntime = agentRuntime;
    }

    @Override
    public String getName() {
        return "assign_sub_assistant";
    }

    @Override
    public String getDescription() {
        return "Assign a task to a sub-agent for parallel execution. "
            + "Use this when a task can be broken down and executed independently.";
    }

    @Override
    public Map<String, Object> getParametersSchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");

        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("goal", stringParam("The final goal for the sub-agent", true));
        properties.put("context", stringParam("Additional context or instructions", false));
        schema.put("properties", properties);

        java.util.List<String> required = new java.util.ArrayList<>();
        required.add("goal");
        schema.put("required", required);
        return schema;
    }

    @Override
    public Object execute(Map<String, Object> args, MainAgentState state) {
        String goal = (String) args.get("goal");
        String context = (String) args.getOrDefault("context", "");

        log.info("[SubAgentAssignTool] Assigning task: {}", goal);

        // 构建子 Agent 初始状态
        Map<String, Object> initialState = new LinkedHashMap<>();
        initialState.put("historyId", state.getHistoryId());
        initialState.put("agentRole", "sub_agent");
        initialState.put("agentName", "sub-agent-" + UUID.randomUUID().toString().substring(0, 8));
        initialState.put("finalGoal", goal);
        initialState.put("status", "pending");

        // 提交到 AgentRuntime
        String taskId = agentRuntime.submitSubAgentTask(
            initialState,
            state.getConfig(),
            (String) initialState.get("agentName")
        );

        return "Sub-agent task assigned. Task ID: " + taskId + "\nGoal: " + goal;
    }
}
