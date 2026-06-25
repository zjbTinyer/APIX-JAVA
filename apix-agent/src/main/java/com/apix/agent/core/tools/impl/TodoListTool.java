package com.apix.agent.core.tools.impl;

import com.apix.common.model.MainAgentState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * 待办列表工具 — 对标 Python: tools/basic_tools/todo_list.py :: write_todos
 *
 * 让 Agent 输出结构化待办列表，用于拆解复杂任务。
 */
public class TodoListTool extends BaseTool {

    private static final Logger log = LoggerFactory.getLogger(TodoListTool.class);

    @Override
    public String getName() {
        return "write_todos";
    }

    @Override
    public String getDescription() {
        return "Create a structured todo list to track progress on complex tasks. "
            + "Use this to break down tasks into actionable steps.";
    }

    @Override
    public Map<String, Object> getParametersSchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");

        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("todos", new LinkedHashMap<String, Object>() {{
            put("type", "array");
            put("description", "List of todo items");
            put("items", new LinkedHashMap<String, Object>() {{
                put("type", "object");
                put("properties", new LinkedHashMap<String, Object>() {{
                    put("content", new LinkedHashMap<String, Object>() {{
                        put("type", "string");
                        put("description", "Description of the todo item");
                    }});
                    put("status", new LinkedHashMap<String, Object>() {{
                        put("type", "string");
                        put("enum", Arrays.asList("pending", "in_progress", "completed"));
                        put("description", "Current status of the todo item");
                    }});
                }});
                put("required", Arrays.asList("content", "status"));
            }});
        }});
        schema.put("properties", properties);
        schema.put("required", Collections.singletonList("todos"));
        return schema;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object execute(Map<String, Object> args, MainAgentState state) {
        Object todosObj = args.get("todos");
        if (todosObj == null) {
            return "Error: todos is required";
        }

        List<Map<String, Object>> todos;
        if (todosObj instanceof List) {
            todos = (List<Map<String, Object>>) todosObj;
        } else {
            return "Error: todos must be an array";
        }

        log.info("[TodoListTool] Received {} todos", todos.size());

        StringBuilder result = new StringBuilder("Todo list updated:\n");
        for (int i = 0; i < todos.size(); i++) {
            Map<String, Object> item = todos.get(i);
            String content = (String) item.getOrDefault("content", "");
            String status = (String) item.getOrDefault("status", "pending");
            result.append(i + 1).append(". [").append(status).append("] ").append(content).append("\n");
        }

        return result.toString();
    }
}
