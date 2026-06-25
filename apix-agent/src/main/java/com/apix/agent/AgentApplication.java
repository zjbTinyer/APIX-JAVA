package com.apix.agent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * APIX Agent 服务入口 — 对标 Python: main.py
 *
 * 启动 Agent 运行时，监听 WebSocket 和 HTTP 请求。
 */
@SpringBootApplication
public class AgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(AgentApplication.class, args);
    }
}
