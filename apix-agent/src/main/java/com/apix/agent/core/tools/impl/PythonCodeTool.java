package com.apix.agent.core.tools.impl;

import com.apix.common.model.MainAgentState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Python 代码执行工具 — 对标 Python: tools/code_runner/python_code_runner.py :: run_python_code
 *
 * 在 Docker 沙箱或本地执行 Python 代码片段。
 */
public class PythonCodeTool extends BaseTool {

    private static final Logger log = LoggerFactory.getLogger(PythonCodeTool.class);

    @Override
    public String getName() {
        return "run_python_code";
    }

    @Override
    public String getDescription() {
        return "Execute Python code and return the output. Ideal for data analysis, calculations, and automation.";
    }

    @Override
    public Map<String, Object> getParametersSchema() {
        return singleParamSchema("code", "Python code to execute");
    }

    @Override
    public Object execute(Map<String, Object> args, MainAgentState state) {
        String code = (String) args.get("code");
        String workDir = state.getConfig() != null ? state.getConfig().getWorkDir() : ".";

        if (code == null || code.isEmpty()) {
            return "Error: code is required";
        }

        log.info("[PythonCodeTool] Executing Python code ({} chars)", code.length());

        try {
            ProcessBuilder pb = new ProcessBuilder("python3", "-c", code);
            pb.directory(new java.io.File(workDir));
            pb.redirectErrorStream(true);

            Process process = pb.start();
            boolean finished = process.waitFor(30, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return "Error: Python execution timed out (30s)";
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
            String result = output.toString().trim();
            if (result.length() > 32000) {
                result = result.substring(0, 32000) + "\n... (truncated)";
            }

            return "Exit code: " + exitCode + "\n" + result;

        } catch (Exception e) {
            log.error("[PythonCodeTool] Failed", e);
            return "Error: " + e.getMessage();
        }
    }
}
