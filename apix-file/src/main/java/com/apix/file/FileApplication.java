package com.apix.file;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * APIX File 服务入口 — 对标 Python: FILE/file_service/main.py
 *
 * 文件上传/下载、RAG 知识库管理。
 */
@SpringBootApplication
@MapperScan("com.apix.file.mapper")
@EnableTransactionManagement
public class FileApplication {
    public static void main(String[] args) {
        SpringApplication.run(FileApplication.class, args);
    }
}
