package com.apix.agent.core.tools.impl;

import com.apix.common.model.MainAgentState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 工作区命令执行工具 — 对标 Python: tools/code_runner/cmd.py :: run_workspace_command
 *
 * 在工作目录中执行 shell 命令并返回输出。
 */
public class CommandTool extends BaseTool {

    private static final Logger log = LoggerFactory.getLogger(CommandTool.class);

    @Override
    public String getName() {
        return "run_workspace_command";
    }

    @Override
    public String getDescription() {
        return "Run a shell command in the workspace directory. Returns stdout and stderr.";
    }

    @Override
    public Map<String, Object> getParametersSchema() {
        return singleParamSchema("command", "Shell command to execute");
    }

    @Override
    public Object execute(Map<String, Object> args, MainAgentState state) {
        String command = (String) args.get("command");
        String workDir = state.getConfig() != null ? state.getConfig().getWorkDir() : ".";

        if (command == null || command.isEmpty()) {
            return "Error: command is required";
        }

        log.info("[CommandTool] Executing: {} (in {})", command, workDir);

        try {
            ProcessBuilder pb = new ProcessBuilder();
            pb.directory(new java.io.File(workDir));

            // 根据 OS 选择 shell
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) {
                pb.command("cmd.exe", "/c", command);
            } else {
                pb.command("bash", "-c", command);
            }

            pb.redirectErrorStream(true);
            Process process = pb.start();

            boolean finished = process.waitFor(30, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return "Error: Command timed out (30s)";
            }

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            int exitCode = process.exitValue();
            log.info("[CommandTool] Exit code: {}, output length: {}", exitCode, output.length());

            String result = output.toString().trim();
            if (result.length() > 32000) {
                result = result.substring(0, 32000) + "\n... (truncated)";
            }

            return "Exit code: " + exitCode + "\n" + result;

        } catch (Exception e) {
            log.error("[CommandTool] Failed: {}", command, e);
            return "Error: " + e.getMessage();
        }
    }
}
