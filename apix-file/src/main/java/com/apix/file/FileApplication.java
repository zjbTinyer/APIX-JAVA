package com.apix.file;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * APIX File 服务入口 — 对标 Python: FILE/file_service/main.py
 *
 * 文件上传/下载、RAG 知识库管理。
 */
@SpringBootApplication
public class FileApplication {
    public static void main(String[] args) {
        SpringApplication.run(FileApplication.class, args);
    }
}
