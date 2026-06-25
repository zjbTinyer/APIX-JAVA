package com.apix.common.constant;

/**
 * Agent 全局常量 — 对标 Python: global_config.py
 */
public interface AgentConstants {

    String OPERATION_SYSTEM = System.getProperty("os.name").toLowerCase();

    boolean DEBUG = true;
    boolean TRACE = true;

    String BASE_DIR = "../apix_running_time/";
    String SANDBOX_DOCKER_IMAGE_NAME = "agent-sandbox:latest";

    int TOOLS_MAX_OUTPUT_LENGTH = 32000;
    int MAX_RETRY = 8;
    int GENERATION_TTL = 600; // 已完成/已中止生成的清理时间（秒）
    int CONTAINER_TTL = 6000; // 容器 TTL
    int GRAPH_CACHE_TTL = 600; // 图缓存 TTL
    int CACHE_CLEAN_INTERVAL = 300; // 缓存清理间隔

    // 服务端口
    int AGENT_SERVICE_PORT = 5091;
    int MEMORY_SERVICE_PORT = 5093;
    int FILE_SERVICE_PORT = 5094;
    int TASK_SERVICE_PORT = 5090;

    // 服务地址
    String AGENT_SERVICE_URL = "http://localhost:5091";
    String MEMORY_SERVICE_BASE_URL = "http://localhost:5093";
    String FILE_SERVICE_URL = "http://localhost:5094";
    String TASK_SERVER_BASE_URL = "http://localhost:5090";
}
