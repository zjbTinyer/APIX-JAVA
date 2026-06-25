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
 * 移动/重命名工作区文件 — 对标 Python: tools/basic_tools/file_manager.py :: move_workspace_file
 */
public class MoveFileTool extends BaseTool {

    private static final Logger log = LoggerFactory.getLogger(MoveFileTool.class);

    @Override
    public String getName() {
        return "move_workspace_file";
    }

    @Override
    public String getDescription() {
        return "Move or rename a file or directory in the workspace.";
    }

    @Override
    public Map<String, Object> getParametersSchema() {
        Map<String, Object> schema = new java.util.LinkedHashMap<>();
        schema.put("type", "object");

        Map<String, Object> properties = new java.util.LinkedHashMap<>();
        properties.put("source", stringParam("Source relative path", true));
        properties.put("destination", stringParam("Destination relative path", true));
        schema.put("properties", properties);

        java.util.List<String> required = new java.util.ArrayList<>();
        required.add("source");
        required.add("destination");
        schema.put("required", required);
        return schema;
    }

    @Override
    public Object execute(Map<String, Object> args, MainAgentState state) {
        String source = (String) args.get("source");
        String dest = (String) args.get("destination");
        String workDir = state.getConfig() != null ? state.getConfig().getWorkDir() : "";

        if (source == null || dest == null) {
            return "Error: source and destination are required";
        }

        try {
            Path srcPath = Paths.get(workDir, source).normalize();
            Path destPath = Paths.get(workDir, dest).normalize();

            if (!Files.exists(srcPath)) {
                return "Error: Source not found: " + source;
            }

            Files.createDirectories(destPath.getParent());
            Files.move(srcPath, destPath);

            log.info("[MoveFileTool] Moved: {} → {}", source, dest);
            return "Moved " + source + " → " + dest;

        } catch (IOException e) {
            log.error("[MoveFileTool] Failed to move: {} → {}", source, dest, e);
            return "Error moving: " + e.getMessage();
        }
    }
}
