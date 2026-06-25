package com.apix.agent.core.sandbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Docker 沙箱管理器（真实 Docker 命令实现）— 对标 Python: AgentSandboxManager
 *
 * 通过执行 docker CLI 命令管理容器生命周期。
 * 每个 (clientId + workDir) 对应一个沙箱容器。
 */
@Component
public class DockerSandboxManager {

    private static final Logger log = LoggerFactory.getLogger(DockerSandboxManager.class);

    @Value("${apix.agent.sandbox-image:agent-sandbox:latest}")
    private String sandboxImage;

    @Value("${apix.agent.container-ttl:6000}")
    private long containerTtlMs;

    private final Map<String, ContainerInfo> containers = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        containerTtlMs = TimeUnit.SECONDS.toMillis(
            containerTtlMs > 0 ? containerTtlMs : 6000);
        log.info("[DockerSandbox] Initialized with image={}, ttl={}ms", sandboxImage, containerTtlMs);
    }

    @PreDestroy
    public void cleanup() {
        log.info("[DockerSandbox] Cleaning up all containers...");
        for (Map.Entry<String, ContainerInfo> entry : containers.entrySet()) {
            try {
                stopAndRemoveContainer(entry.getValue().containerId);
            } catch (Exception e) {
                log.warn("[DockerSandbox] Cleanup failed for {}", entry.getValue().containerId, e);
            }
        }
        containers.clear();
    }

    /**
     * 获取或创建沙箱容器。
     */
    public String getOrCreateSandbox(String clientId, String workDir) {
        if (workDir == null || workDir.isEmpty() || !Files.exists(Paths.get(workDir))) {
            return "";
        }

        String key = clientId + ":" + Paths.get(workDir).toAbsolutePath().normalize();

        ContainerInfo entry = containers.get(key);
        if (entry != null && isContainerRunning(entry.containerId)) {
            entry.expireAt = System.currentTimeMillis() + containerTtlMs;
            return entry.containerId;
        }

        // 创建新容器
        String containerId = createContainer(workDir);
        if (containerId != null) {
            containers.put(key, new ContainerInfo(containerId));
            log.info("[DockerSandbox] Created container: {} for key={}", containerId, key);
        }
        return containerId;
    }

    /**
     * 执行命令到沙箱容器。
     */
    public String execInSandbox(String containerId, String command) {
        try {
            String[] cmd = {"docker", "exec", containerId, "bash", "-c", command};
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);

            Process process = pb.start();
            boolean finished = process.waitFor(30, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return "Command timed out (30s)";
            }

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }
            return output.toString().trim();

        } catch (Exception e) {
            log.error("[DockerSandbox] exec failed", e);
            return "Error: " + e.getMessage();
        }
    }

    /**
     * 销毁沙箱。
     */
    public void destroySandbox(String clientId, String workDir) {
        String key = clientId + ":" + Paths.get(workDir).toAbsolutePath().normalize();
        ContainerInfo entry = containers.remove(key);
        if (entry != null) {
            stopAndRemoveContainer(entry.containerId);
        }
    }

    /**
     * 通过 docker CLI 创建容器。
     */
    private String createContainer(String workDir) {
        try {
            Path workPath = Paths.get(workDir).toAbsolutePath().normalize();
            String containerName = "apix-sandbox-" + Long.toHexString(System.nanoTime());

            String[] cmd = {
                "docker", "run", "-d",
                "--name", containerName,
                "-v", workPath + ":/workspace",
                "-w", "/workspace",
                "--restart", "no",
                sandboxImage,
                "tail", "-f", "/dev/null"  // 保持容器运行
            };

            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            boolean finished = process.waitFor(30, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return null;
            }

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String containerId = reader.readLine();
                if (containerId != null && !containerId.isEmpty()) {
                    log.info("[DockerSandbox] Container created: {} (name={})", containerId.trim(), containerName);
                    return containerId.trim();
                }
            }

            return null;

        } catch (Exception e) {
            log.error("[DockerSandbox] Failed to create container", e);
            return null;
        }
    }

    private boolean isContainerRunning(String containerId) {
        try {
            String[] cmd = {"docker", "inspect", "-f", "{{.State.Running}}", containerId};
            ProcessBuilder pb = new ProcessBuilder(cmd);
            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String status = reader.readLine();
                return "true".equals(status);
            }

        } catch (Exception e) {
            return false;
        }
    }

    private void stopAndRemoveContainer(String containerId) {
        try {
            // 先尝试停止
            new ProcessBuilder("docker", "stop", containerId)
                .start().waitFor(10, TimeUnit.SECONDS);
            // 再删除
            new ProcessBuilder("docker", "rm", containerId)
                .start().waitFor(10, TimeUnit.SECONDS);
            log.info("[DockerSandbox] Removed container: {}", containerId);

        } catch (Exception e) {
            log.warn("[DockerSandbox] Failed to remove container: {}", containerId, e);
        }
    }

    private static class ContainerInfo {
        final String containerId;
        long expireAt;

        ContainerInfo(String containerId) {
            this.containerId = containerId;
            this.expireAt = System.currentTimeMillis() +
                TimeUnit.SECONDS.toMillis(6000);
        }
    }
}
