package com.apix.agent.core.tools.impl;

import com.apix.common.model.MainAgentState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 列出工作区文件 — 对标 Python: tools/basic_tools/file_manager.py :: list_workspace_files
 */
public class ListFilesTool extends BaseTool {

    private static final Logger log = LoggerFactory.getLogger(ListFilesTool.class);

    @Override
    public String getName() {
        return "list_workspace_files";
    }

    @Override
    public String getDescription() {
        return "List files and directories in the workspace. Specify a path to list a subdirectory.";
    }

    @Override
    public Map<String, Object> getParametersSchema() {
        Map<String, Object> schema = new java.util.LinkedHashMap<>();
        schema.put("type", "object");

        Map<String, Object> properties = new java.util.LinkedHashMap<>();
        properties.put("path", stringParam("Relative path to list (default: root)", false));
        properties.put("recursive", stringParam("Whether to list recursively (default: false)", false));
        schema.put("properties", properties);
        schema.put("required", java.util.Collections.emptyList());
        return schema;
    }

    @Override
    public Object execute(Map<String, Object> args, MainAgentState state) {
        String subPath = (String) args.getOrDefault("path", "");
        boolean recursive = "true".equalsIgnoreCase((String) args.get("recursive"));
        String workDir = state.getConfig() != null ? state.getConfig().getWorkDir() : "";

        try {
            Path dirPath = Paths.get(workDir, subPath).normalize();
            if (!Files.exists(dirPath) || !Files.isDirectory(dirPath)) {
                return "Error: Directory not found: " + (subPath.isEmpty() ? "(root)" : subPath);
            }

            if (recursive) {
                // 递归遍历
                List<Map<String, Object>> entries = new ArrayList<>();
                Files.walkFileTree(dirPath, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                        Map<String, Object> entry = new LinkedHashMap<>();
                        entry.put("name", dirPath.relativize(file).toString());
                        entry.put("type", "file");
                        entry.put("size", attrs.size());
                        entries.add(entry);
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                        if (!dir.equals(dirPath)) {
                            Map<String, Object> entry = new LinkedHashMap<>();
                            entry.put("name", dirPath.relativize(dir).toString() + "/");
                            entry.put("type", "dir");
                            entries.add(entry);
                        }
                        return FileVisitResult.CONTINUE;
                    }
                });
                return entries;
            } else {
                // 仅当前目录
                try (Stream<Path> stream = Files.list(dirPath)) {
                    List<Map<String, Object>> entries = stream
                        .map(p -> {
                            Map<String, Object> entry = new LinkedHashMap<>();
                            entry.put("name", p.getFileName().toString());
                            entry.put("type", Files.isDirectory(p) ? "dir" : "file");
                            try {
                                if (!Files.isDirectory(p)) entry.put("size", Files.size(p));
                            } catch (IOException ignored) {}
                            return entry;
                        })
                        .collect(Collectors.toList());
                    return entries;
                }
            }

        } catch (IOException e) {
            log.error("[ListFilesTool] Failed to list: {}", subPath, e);
            return "Error listing directory: " + e.getMessage();
        }
    }
}
