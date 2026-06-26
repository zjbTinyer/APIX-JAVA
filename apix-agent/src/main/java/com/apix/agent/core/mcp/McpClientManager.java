package com.apix.agent.core.mcp;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * MCP (Model Context Protocol) 客户端管理器 — 对标 Python: tools/mcp/mcp_tool.py
 *
 * 支持多种 MCP 传输协议：
 * - SSE（Server-Sent Events）
 * - Stdio（子进程 stdio）
 * - Streamable HTTP
 * - WebSocket
 *
 * 每个 MCP 连接对外暴露为工具集，注册到 ToolRegistry。
 */
@Component
public class McpClientManager {

    private static final Logger log = LoggerFactory.getLogger(McpClientManager.class);

    /** clientId -> MCP 连接列表 */
    private final Map<String, List<McpConnection>> connections = new ConcurrentHashMap<>();

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(java.time.Duration.ofSeconds(10))
            .build();

    @PreDestroy
    public void cleanup() {
        connections.values().forEach(list -> list.forEach(McpConnection::close));
        connections.clear();
    }

    /**
     * 添加 SSE 连接。
     */
    public void addSseConnection(String clientId, String name, String url) {
        McpConnection conn = new McpConnection(name, TransportType.SSE, url, null);
        connections.computeIfAbsent(clientId, k -> new ArrayList<>()).add(conn);
        log.info("[MCP] Added SSE connection: {} → {} for client={}", name, url, clientId);
    }

    /**
     * 添加 Stdio 连接。
     */
    public void addStdioConnection(String clientId, String name, String command) {
        McpConnection conn = new McpConnection(name, TransportType.STDIO, null, command);
        connections.computeIfAbsent(clientId, k -> new ArrayList<>()).add(conn);
        log.info("[MCP] Added Stdio connection: {} → {} for client={}", name, command, clientId);
    }

    /**
     * 添加 Streamable HTTP 连接。
     */
    public void addHttpConnection(String clientId, String name, String url) {
        McpConnection conn = new McpConnection(name, TransportType.HTTP, url, null);
        connections.computeIfAbsent(clientId, k -> new ArrayList<>()).add(conn);
        log.info("[MCP] Added HTTP connection: {} → {} for client={}", name, url, clientId);
    }

    /**
     * 加载指定客户端的所有 MCP 工具。
     */
    public List<McpToolInfo> loadAllMcpTools(String clientId) {
        List<McpToolInfo> allTools = new ArrayList<>();
        List<McpConnection> conns = connections.get(clientId);
        if (conns == null)
            return allTools;

        for (McpConnection conn : conns) {
            try {
                List<McpToolInfo> tools = discoverTools(conn);
                allTools.addAll(tools);
            } catch (Exception e) {
                log.warn("[MCP] Failed to discover tools from {}: {}", conn.name, e.getMessage());
            }
        }
        return allTools;
    }

    /**
     * 执行 MCP 工具调用。
     */
    public String executeTool(String clientId, String toolName, Map<String, Object> args) {
        List<McpConnection> conns = connections.get(clientId);
        if (conns == null) {
            return "Error: No MCP connections for client " + clientId;
        }

        for (McpConnection conn : conns) {
            try {
                // 遍历所有连接，找到能处理该工具的
                return callTool(conn, toolName, args);
            } catch (Exception e) {
                log.debug("[MCP] Connection {} cannot handle tool {}: {}",
                        conn.name, toolName, e.getMessage());
            }
        }

        return "Error: Tool " + toolName + " not found in any MCP connection";
    }

    /**
     * 发现连接的可用工具。
     */
    private List<McpToolInfo> discoverTools(McpConnection conn) throws Exception {
        switch (conn.transportType) {
            case SSE:
            case HTTP:
                return discoverViaHttp(conn);
            case STDIO:
                return discoverViaStdio(conn);
            default:
                return Collections.emptyList();
        }
    }

    /**
     * 通过 HTTP 发现工具（SSE / HTTP 传输）。
     */
    private List<McpToolInfo> discoverViaHttp(McpConnection conn) throws Exception {
        JSONObject request = new JSONObject();
        request.put("jsonrpc", "2.0");
        request.put("method", "tools/list");
        request.put("id", UUID.randomUUID().toString());

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(conn.url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(request.toJSONString()))
                .timeout(java.time.Duration.ofSeconds(10))
                .build();

        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        JSONObject json = JSON.parseObject(resp.body());

        List<McpToolInfo> tools = new ArrayList<>();
        JSONArray resultArray = json.getJSONObject("result") != null
                ? json.getJSONObject("result").getJSONArray("tools")
                : null;
        if (resultArray == null)
            return tools;

        for (int i = 0; i < resultArray.size(); i++) {
            JSONObject t = resultArray.getJSONObject(i);
            McpToolInfo info = new McpToolInfo();
            info.name = conn.name + "_" + t.getString("name");
            info.description = t.getString("description");
            info.inputSchema = t.getJSONObject("inputSchema");
            info.connectionName = conn.name;
            tools.add(info);
        }

        return tools;
    }

    /**
     * 通过 Stdio 发现工具 — 启动子进程并通过 JSON-RPC over stdio 通信。
     */
    private List<McpToolInfo> discoverViaStdio(McpConnection conn) throws Exception {
        log.info("[MCP] Stdio discovery for {}: command={}", conn.name, conn.command);
        List<McpToolInfo> tools = new ArrayList<>();

        Process process = startStdioProcess(conn);
        if (process == null)
            return tools;

        // 发送 tools/list 请求
        JSONObject request = new JSONObject();
        request.put("jsonrpc", "2.0");
        request.put("method", "tools/list");
        request.put("id", "1");

        String response = sendStdioRequest(process, request.toJSONString());
        if (response == null)
            return tools;

        JSONObject json = JSON.parseObject(response);
        JSONArray resultArray = json.getJSONObject("result") != null
                ? json.getJSONObject("result").getJSONArray("tools")
                : null;
        if (resultArray == null)
            return tools;

        for (int i = 0; i < resultArray.size(); i++) {
            JSONObject t = resultArray.getJSONObject(i);
            McpToolInfo info = new McpToolInfo();
            info.name = conn.name + "_" + t.getString("name");
            info.description = t.getString("description");
            info.inputSchema = t.getJSONObject("inputSchema");
            info.connectionName = conn.name;
            tools.add(info);
        }

        log.info("[MCP] Stdio discovered {} tools from {}", tools.size(), conn.name);
        return tools;
    }

    /**
     * 通过 Stdio 调用工具。
     */
    private String callViaStdio(McpConnection conn, String toolName, Map<String, Object> args) throws Exception {
        log.info("[MCP] Stdio call: {} → {} on {}", toolName, conn.name);

        Process process = startStdioProcess(conn);
        if (process == null)
            return "Error: Failed to start stdio process for " + conn.name;

        JSONObject params = new JSONObject();
        params.put("name", toolName);
        params.put("arguments", args);

        JSONObject request = new JSONObject();
        request.put("jsonrpc", "2.0");
        request.put("method", "tools/call");
        request.put("id", "2");
        request.put("params", params);

        String response = sendStdioRequest(process, request.toJSONString());
        if (response == null)
            return "Error: No response from " + conn.name;

        JSONObject json = JSON.parseObject(response);
        JSONObject result = json.getJSONObject("result");
        if (result != null) {
            JSONObject content = result.getJSONObject("content");
            return content != null ? content.toJSONString() : result.toJSONString();
        }
        JSONObject err = json.getJSONObject("error");
        return err != null ? "Error: " + err.toJSONString() : response;
    }

    /**
     * 启动 Stdio 子进程。
     */
    private Process startStdioProcess(McpConnection conn) {
        try {
            if (conn.process != null && conn.process.isAlive()) {
                return conn.process;
            }
            // 解析命令和参数
            String[] parts = conn.command.split("\\s+");
            ProcessBuilder pb = new ProcessBuilder(parts);
            pb.redirectErrorStream(false);
            conn.process = pb.start();
            log.info("[MCP] Started stdio process: {}", conn.command);
            return conn.process;
        } catch (Exception e) {
            log.error("[MCP] Failed to start stdio process: {}", conn.command, e);
            return null;
        }
    }

    /**
     * 通过子进程 stdio 发送 JSON-RPC 请求并读取响应。
     * 写入 stdin → 读取 stdout 的第一行作为 JSON 响应。
     */
    private String sendStdioRequest(Process process, String requestJson) {
        try {
            // 写入 stdin
            BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(process.getOutputStream(), java.nio.charset.StandardCharsets.UTF_8));
            writer.write(requestJson);
            writer.newLine();
            writer.flush();

            // 读取 stdout（读取所有行）
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder response = new StringBuilder();
            String line;
            long deadline = System.currentTimeMillis() + 10000; // 10s 超时
            while (System.currentTimeMillis() < deadline && (line = reader.readLine()) != null) {
                response.append(line);
            }

            return response.length() > 0 ? response.toString() : null;

        } catch (Exception e) {
            log.warn("[MCP] Stdio communication error: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 调用工具。
     */
    private String callTool(McpConnection conn, String toolName, Map<String, Object> args) throws Exception {
        // 去掉连接名前缀
        String rawName = toolName;
        if (toolName.startsWith(conn.name + "_")) {
            rawName = toolName.substring(conn.name.length() + 1);
        }

        switch (conn.transportType) {
            case SSE:
            case HTTP:
                return callViaHttp(conn, rawName, args);
            case STDIO:
                return callViaStdio(conn, rawName, args);
            default:
                return "Unsupported transport";
        }
    }

    private String callViaHttp(McpConnection conn, String toolName, Map<String, Object> args) throws Exception {
        JSONObject request = new JSONObject();
        request.put("jsonrpc", "2.0");
        request.put("method", "tools/call");
        request.put("id", UUID.randomUUID().toString());

        JSONObject params = new JSONObject();
        params.put("name", toolName);
        params.put("arguments", args);
        request.put("params", params);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(conn.url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(request.toJSONString()))
                .timeout(java.time.Duration.ofSeconds(30))
                .build();

        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        JSONObject json = JSON.parseObject(resp.body());
        JSONObject result = json.getJSONObject("result");

        if (result != null) {
            JSONObject content = result.getJSONObject("content");
            return content != null ? content.toJSONString() : result.toJSONString();
        }

        JSONObject err = json.getJSONObject("error");
        return err != null ? "Error: " + err.toJSONString() : "Unknown MCP response";
    }

    // ==================== 内部类型 ====================

    public enum TransportType {
        SSE, STDIO, HTTP, WEBSOCKET
    }

    private static class McpConnection {
        final String name;
        final TransportType transportType;
        final String url;
        final String command;
        Process process;

        McpConnection(String name, TransportType type, String url, String command) {
            this.name = name;
            this.transportType = type;
            this.url = url;
            this.command = command;
        }

        void close() {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }

    /**
     * MCP 工具信息。
     */
    public static class McpToolInfo {
        public String name;
        public String description;
        public JSONObject inputSchema;
        public String connectionName;
    }
}
