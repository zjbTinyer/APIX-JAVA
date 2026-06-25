package com.apix.agent.core.tools;

import com.apix.common.model.MainAgentState;

import java.util.Map;

/**
 * Agent 工具接口 — 对标 Python @tool 装饰器。
 *
 * 所有工具类实现此接口，注册到 ToolRegistry。
 */
public interface ApixTool {

    /**
     * 工具名称（LLM 通过此名称调用工具）。
     */
    String getName();

    /**
     * 工具描述（用于 LLM 理解工具的用途）。
     */
    String getDescription();

    /**
     * 工具参数 schema（JSON Schema 格式，用于 LLM 生成参数）。
     */
    Map<String, Object> getParametersSchema();

    /**
     * 执行工具。
     *
     * @param args  工具参数
     * @param state 当前 Agent 状态
     * @return 执行结果
     */
    Object execute(Map<String, Object> args, MainAgentState state);
}
