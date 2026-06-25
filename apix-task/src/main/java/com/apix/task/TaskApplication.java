package com.apix.task;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * APIX Task 服务入口 — 对标 Python: TASK/task_flow_module/main.py
 *
 * 任务流引擎，管理自动化任务流的定义和执行。
 */
@SpringBootApplication
public class TaskApplication {
    public static void main(String[] args) {
        SpringApplication.run(TaskApplication.class, args);
    }
}
