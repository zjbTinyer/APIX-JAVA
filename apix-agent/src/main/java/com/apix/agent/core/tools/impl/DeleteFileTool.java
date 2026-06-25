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
 * 删除工作区文件 — 对标 Python: tools/basic_tools/file_manager.py :: delete_workspace_file
 */
public class DeleteFileTool extends BaseTool {

    private static final Logger log = LoggerFactory.getLogger(DeleteFileTool.class);

    @Override
    public String getName() {
        return "delete_workspace_file";
    }

    @Override
    public String getDescription() {
        return "Delete a file or empty directory in the workspace.";
    }

    @Override
    public Map<String, Object> getParametersSchema() {
        return singleParamSchema("file_path", "Relative path to the file or directory to delete");
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
                return "Error: Not found: " + filePath;
            }

            if (Files.isDirectory(fullPath)) {
                Files.delete(fullPath);
            } else {
                Files.delete(fullPath);
            }

            log.info("[DeleteFileTool] Deleted: {}", filePath);
            return "Deleted: " + filePath;

        } catch (IOException e) {
            log.error("[DeleteFileTool] Failed to delete: {}", filePath, e);
            return "Error deleting: " + e.getMessage();
        }
    }
}
