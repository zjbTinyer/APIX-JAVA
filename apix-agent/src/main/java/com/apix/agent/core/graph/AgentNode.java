package com.apix.agent.core.graph;

import com.apix.common.model.MainAgentState;

/**
 * Agent 图节点接口 — 对标 LangGraph 的节点抽象。
 *
 * 每个节点接收当前状态，处理后返回更新后的状态和路由指令。
 */
public interface AgentNode {

    /**
     * 执行节点逻辑。
     *
     * @param state 当前图状态
     * @return 路由指令（下一个节点名或 END）
     */
    String execute(MainAgentState state);

    /**
     * 节点名称。
     */
    String getName();
}
