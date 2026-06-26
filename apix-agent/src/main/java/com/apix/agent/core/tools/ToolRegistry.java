package com.apix.agent.core.tools;

import com.apix.agent.core.AgentRuntime;
import com.apix.agent.core.tools.impl.*;
import com.apix.common.model.MainAgentState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 工具注册表 — 对标 Python: tools/registry.py
 *
 * 单例模式，管理所有可用工具。
 * 支持按权限过滤工具列表。
 */
@Component
public class ToolRegistry {

    private static final Logger log = LoggerFactory.getLogger(ToolRegistry.class);

    private final Map<String, ApixTool> tools = new ConcurrentHashMap<>();

    @Autowired(required = false)
    private AgentRuntime agentRuntime;

    /** 子 Agent 禁止使用的工具 */
    public static final Set<String> FORBIDDEN_FOR_SUB_AGENT = Set.of(
            "request_user_input", "send_images", "assign_sub_assistant",
            "query_sub_assistant", "stop_sub_assistant");

    /** 需要工作目录的工具 */
    public static final Set<String> NEED_WORKSPACE_TOOLS = Set.of(
            "fetch_files", "read_workspace_file", "list_workspace_files",
            "write_workspace_file", "move_workspace_file", "delete_workspace_file",
            "run_workspace_command", "run_python_code", "load_skill",
            "ocr_analysis", "send_images");

    /** 冲突工具集（同一轮不能同时调用） */
    public static final Set<String> CONFLICT_TOOL_SET = Set.of(
            "write_todos", "update_memory", "load_skill");

    /** 权限 → 工具名称映射 */
    private static final Map<String, Set<String>> PERMISSION_TOOL_MAP = new LinkedHashMap<>();

    static {
        PERMISSION_TOOL_MAP.put("file_operation", Set.of(
                "fetch_files", "read_workspace_file", "list_workspace_files",
                "write_workspace_file", "move_workspace_file", "delete_workspace_file"));
        PERMISSION_TOOL_MAP.put("web_search", Set.of(
                "search_web_by_keywords", "search_web_by_urls"));
        PERMISSION_TOOL_MAP.put("knowledge_retrieval", Set.of(
                "search_knowledge_base"));
        PERMISSION_TOOL_MAP.put("command_operation", Set.of(
                "run_workspace_command", "run_python_code"));
        PERMISSION_TOOL_MAP.put("skill_load", Set.of(
                "load_skill"));
        PERMISSION_TOOL_MAP.put("agent_assign", Set.of(
                "assign_sub_assistant", "query_sub_assistant", "stop_sub_assistant"));
        PERMISSION_TOOL_MAP.put("task_flow", Set.of(
                "update_test_task", "get_test_task"));
        PERMISSION_TOOL_MAP.put("default", Set.of(
                "write_todos", "read_memory", "update_memory",
                "ocr_analysis", "send_images", "request_user_input"));
    }

    @PostConstruct
    public void init() {
        registerBuiltinTools();
    }

    /**
     * 注册一个工具。
     */
    public void register(ApixTool tool) {
        tools.put(tool.getName(), tool);
        log.info("[ToolRegistry] Registered tool: {}", tool.getName());
    }

    /**
     * 获取工具。
     */
    public ApixTool getTool(String name) {
        return tools.get(name);
    }

    /**
     * 执行工具。
     */
    public Object execute(String name, Map<String, Object> args, MainAgentState state) {
        ApixTool tool = tools.get(name);
        if (tool == null) {
            throw new IllegalArgumentException("Tool not found: " + name);
        }
        return tool.execute(args, state);
    }

    /**
     * 根据权限获取可用工具列表（用于 LLM function calling）。
     */
    public List<Map<String, Object>> getToolsForPermissions(List<String> permissions,
            String agentRole,
            boolean workspaceConfigured) {
        List<Map<String, Object>> result = new ArrayList<>();

        if (permissions.contains("forbidden")) {
            return result;
        }

        // 收集所有权限允许的工具名称
        Set<String> allowedNames = new HashSet<>();
        for (String perm : permissions) {
            Set<String> names = PERMISSION_TOOL_MAP.get(perm);
            if (names != null) {
                allowedNames.addAll(names);
            }
        }

        for (ApixTool tool : tools.values()) {
            String name = tool.getName();

            // 默认工具始终可用
            boolean isDefault = PERMISSION_TOOL_MAP.getOrDefault("default", Set.of()).contains(name);

            // 检查权限
            if (!isDefault && !allowedNames.contains(name)) {
                continue;
            }

            // 子 Agent 限制
            if (("sub_agent".equals(agentRole) || "team_worker".equals(agentRole))
                    && FORBIDDEN_FOR_SUB_AGENT.contains(name)) {
                continue;
            }

            // 需要工作目录
            if (NEED_WORKSPACE_TOOLS.contains(name) && !workspaceConfigured) {
                continue;
            }

            // 构造 function calling schema
            Map<String, Object> functionSchema = new LinkedHashMap<>();
            functionSchema.put("name", tool.getName());
            functionSchema.put("description", tool.getDescription());
            functionSchema.put("parameters", tool.getParametersSchema());

            result.add(functionSchema);
        }

        return result;
    }

    /**
     * 注册内置工具。
     */
    private void registerBuiltinTools() {
        // 文件操作
        register(new ReadFileTool());
        register(new WriteFileTool());
        register(new ListFilesTool());
        register(new DeleteFileTool());
        register(new MoveFileTool());

        // 代码执行
        register(new CommandTool());
        register(new PythonCodeTool());

        // 网络搜索
        register(new WebSearchTool());

        // 待办列表
        register(new TodoListTool());

        // 子 Agent 分配
        if (agentRuntime != null) {
            register(new SubAgentAssignTool(agentRuntime));
        }

        log.info("[ToolRegistry] Registered {} built-in tools", tools.size());
    }

    /**
     * 获取所有已注册的工具名称。
     */
    public Set<String> getRegisteredToolNames() {
        return tools.keySet();
    }
}
