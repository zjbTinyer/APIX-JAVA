package com.apix.task.controller;

import com.apix.common.model.R;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 任务管理 API — 对标前端 TaskPage
 *
 * 管理自动化任务流的定义和执行。当前为基础版本，后续对接后端任务引擎。
 */
@RestController
@RequestMapping("/api/v1")
public class TaskController {

    private static final Logger log = LoggerFactory.getLogger(TaskController.class);

    /**
     * 健康检查。
     */
    @GetMapping("/health")
    public R<String> health() {
        return R.ok("task-service is running");
    }

    /**
     * 获取任务列表。
     */
    @GetMapping("/tasks")
    public R<List<Map<String, Object>>> getTasks(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search) {
        log.info("[Task] getTasks: status={}, search={}", status, search);
        // 基础版本返回空列表，后续对接后端任务引擎
        return R.ok(Collections.emptyList());
    }

    /**
     * 获取任务统计。
     */
    @GetMapping("/tasks/stats")
    public R<Map<String, Integer>> getTaskStats() {
        Map<String, Integer> stats = new LinkedHashMap<>();
        stats.put("total", 0);
        stats.put("pending", 0);
        stats.put("running", 0);
        stats.put("completed", 0);
        stats.put("failed", 0);
        return R.ok(stats);
    }

    /**
     * 终止任务。
     */
    @PostMapping("/tasks/{taskId}/stop")
    public R<String> stopTask(@PathVariable String taskId) {
        log.info("[Task] stopTask: id={}", taskId);
        return R.ok("stopped");
    }

    /**
     * 清理已完成任务。
     */
    @PostMapping("/tasks/clear-finished")
    public R<String> clearFinishedTasks() {
        log.info("[Task] clearFinishedTasks");
        return R.ok("cleared");
    }
}
