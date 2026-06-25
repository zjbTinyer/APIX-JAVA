package com.apix.common.model;

import java.util.Map;

/**
 * 流式事件写入器接口 — 定义在 common 模块以消除循环依赖。
 *
 * AgentStreamWriter (在 apix-agent 中) 实现此接口。
 * MainAgentState 通过此接口引用 writer，不依赖具体实现。
 */
public interface StreamWriter {

    /**
     * 发送事件到前端。
     *
     * @param eventName 事件名（如 "content_chunk_rtn", "think_chunk_rtn"）
     * @param target    事件目标
     * @param data      事件数据
     */
    void sendEvent(String eventName, ApixEventEnvelopeTarget target, Map<String, Object> data);
}
