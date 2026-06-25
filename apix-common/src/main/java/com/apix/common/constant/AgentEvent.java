package com.apix.common.constant;

/**
 * Agent 事件类型枚举 — 对标 Python: AgentStreamEvent
 */
public interface AgentEvent {

    String ESSENTIAL_INFO_RETURN = "essential_info_return";
    String LLM_STREAM_START = "llm_stream_start";
    String LLM_CHUNK_RETURN = "llm_chunk_return";
    String LLM_STREAM_END = "llm_stream_end";
    String LLM_STREAM_ERROR = "llm_stream_error";
    String AI_MESSAGE_RETURN = "ai_message_return";
    String TOOL_MESSAGE_RETURN = "tool_message_return";
    String TOOL_EXEC_START = "tool_exec_start";
    String TOOL_EXEC_MIDDLE = "tool_exec_middle";
    String TOOL_EXEC_END = "tool_exec_end";
    String RUNTIME_WARNING = "runtime_warning";
    String ERROR_OCCURRED = "error_occurred";
}
