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
 * 写入工作区文件 — 对标 Python: tools/basic_tools/file_manager.py :: write_workspace_file
 */
public class WriteFileTool extends BaseTool {

    private static final Logger log = LoggerFactory.getLogger(WriteFileTool.class);

    @Override
    public String getName() {
        return "write_workspace_file";
    }

    @Override
    public String getDescription() {
        return "Write content to a file in the workspace. Creates directories if needed.";
    }

    @Override
    public Map<String, Object> getParametersSchema() {
        Map<String, Object> schema = new java.util.LinkedHashMap<>();
        schema.put("type", "object");

        Map<String, Object> properties = new java.util.LinkedHashMap<>();
        properties.put("file_path", stringParam("Relative path to the file in the workspace", true));
        properties.put("content", stringParam("Content to write to the file", true));
        schema.put("properties", properties);

        java.util.List<String> required = new java.util.ArrayList<>();
        required.add("file_path");
        required.add("content");
        schema.put("required", required);
        return schema;
    }

    @Override
    public Object execute(Map<String, Object> args, MainAgentState state) {
        String filePath = (String) args.get("file_path");
        String content = (String) args.get("content");
        String workDir = state.getConfig() != null ? state.getConfig().getWorkDir() : "";

        if (filePath == null || filePath.isEmpty()) {
            return "Error: file_path is required";
        }
        if (content == null) content = "";

        try {
            Path fullPath = Paths.get(workDir, filePath).normalize();
            Files.createDirectories(fullPath.getParent());
            Files.write(fullPath, content.getBytes());

            log.info("[WriteFileTool] Wrote file: {} ({} bytes)", filePath, content.length());
            return "File written successfully: " + filePath + " (" + content.length() + " bytes)";

        } catch (IOException e) {
            log.error("[WriteFileTool] Failed to write: {}", filePath, e);
            return "Error writing file: " + e.getMessage();
        }
    }
}
