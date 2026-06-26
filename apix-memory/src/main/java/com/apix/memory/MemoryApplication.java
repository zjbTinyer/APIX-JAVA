package com.apix.memory;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * APIX Memory 服务入口 — 对标 Python: MEMORY/memory_module/main.py
 *
 * 管理对话历史、短期记忆、长期记忆、用户和会话信息。
 */
@SpringBootApplication
@MapperScan("com.apix.memory.mapper")
@EnableTransactionManagement
public class MemoryApplication {
    public static void main(String[] args) {
        SpringApplication.run(MemoryApplication.class, args);
    }
}
