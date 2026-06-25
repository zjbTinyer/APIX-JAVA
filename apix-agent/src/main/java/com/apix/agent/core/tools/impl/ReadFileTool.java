package com.apix.agent.core.tools.impl;

import com.apix.common.model.MainAgentState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

/**
 * 读取工作区文件 — 对标 Python: tools/basic_tools/file_manager.py :: read_workspace_file
 */
public class ReadFileTool extends BaseTool {

    private static final Logger log = LoggerFactory.getLogger(ReadFileTool.class);

    @Override
    public String getName() {
        return "read_workspace_file";
    }

    @Override
    public String getDescription() {
        return "Read the content of a file in the workspace. Returns the file content as text.";
    }

    @Override
    public Map<String, Object> getParametersSchema() {
        Map<String, Object> schema = new java.util.LinkedHashMap<>();
        schema.put("type", "object");

        Map<String, Object> properties = new java.util.LinkedHashMap<>();
        properties.put("file_path", stringParam("Relative path to the file in the workspace", true));
        schema.put("properties", properties);

        schema.put("required", java.util.Collections.singletonList("file_path"));
        return schema;
    }

    @Override
    public Object execute(Map<String, Object> args, MainAgentState state) {
        String filePath = (String) args.get("file_path");
        String workDir = state.getConfig() != null ? state.getConfig().getWorkDir() : "";

        if (filePath == null || filePath.isEmpty()) {
            return "Error: file_path is required";
        }

        try {
            Path fullPath = Paths.get(workDir, filePath).normalize();

            if (!Files.exists(fullPath)) {
                return "Error: File not found: " + filePath;
            }
            if (Files.isDirectory(fullPath)) {
                return "Error: Path is a directory: " + filePath;
            }

            String content = new String(Files.readAllBytes(fullPath));
            log.info("[ReadFileTool] Read file: {} ({} bytes)", filePath, content.length());
            return content;

        } catch (IOException e) {
            log.error("[ReadFileTool] Failed to read: {}", filePath, e);
            return "Error reading file: " + e.getMessage();
        }
    }
}
