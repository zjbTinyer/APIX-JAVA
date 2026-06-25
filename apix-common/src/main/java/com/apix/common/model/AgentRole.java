package com.apix.common.model;

/**
 * Agent 角色枚举 — 对标 Python GraphRuntimeContext.agent_role。
 *
 * - AGENT:       普通角色，直接与用户聊天，无权分配子 Agent
 * - MAIN_AGENT:  主 Agent，可分配一个子 Agent
 * - SUB_AGENT:   子 Agent，执行任务，不直接与用户聊天
 * - TEAM_LEADER: 团队领导，可分配多个子 Agent
 * - TEAM_WORKER: 团队工人，执行任务
 */
public enum AgentRole {

    AGENT("agent"),
    MAIN_AGENT("main_agent"),
    SUB_AGENT("sub_agent"),
    TEAM_LEADER("team_leader"),
    TEAM_WORKER("team_worker");

    private final String value;

    AgentRole(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static AgentRole fromValue(String value) {
        for (AgentRole role : values()) {
            if (role.value.equals(value)) {
                return role;
            }
        }
        return AGENT;
    }
}
