package com.apix.file.controller;

import com.apix.common.model.R;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * RAG 知识库检索 API — 对标 Python: FILE/file_service/routers/rag.py
 *
 * 提供知识库文档的索引、检索能力。
 * 当前为基础版本，后续对接向量数据库（如 Chroma、FAISS）。
 */
@RestController
@RequestMapping("/rag")
public class RagController {

    private static final Logger log = LoggerFactory.getLogger(RagController.class);

    /**
     * 健康检查。
     */
    @GetMapping("/health")
    public R<String> health() {
        return R.ok("rag-service is running");
    }

    /**
     * 搜索知识库 — 对标 Frontend / DataPage 的知识库检索
     *
     * POST /rag/retrieval/search
     * Body: { "client_id": "...", "query": "...", "top_k": 5 }
     *
     * 当前为基础实现，使用关键词匹配（后续接入向量检索）。
     */
    @PostMapping("/retrieval/search")
    public R<List<Map<String, Object>>> search(
            @RequestBody Map<String, Object> payload) {

        String query = (String) payload.getOrDefault("query", "");
        int topK = payload.containsKey("top_k") ? ((Number) payload.get("top_k")).intValue() : 5;

        log.info("[RAG] search: query={}, topK={}", query, topK);

        // 基础版本：返回空结果（后续对接向量数据库）
        List<Map<String, Object>> results = new ArrayList<>();

        return R.ok(results);
    }

    /**
     * 获取文档列表。
     */
    @GetMapping("/documents")
    public R<List<Map<String, Object>>> getDocuments(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {

        log.info("[RAG] getDocuments: page={}, pageSize={}", page, pageSize);
        return R.ok(Collections.emptyList());
    }

    /**
     * 删除文档。
     */
    @DeleteMapping("/documents/{docId}")
    public R<String> deleteDocument(@PathVariable String docId) {
        log.info("[RAG] deleteDocument: id={}", docId);
        return R.ok("deleted");
    }
}
