package com.apix.file.controller;

import com.apix.common.model.R;
import com.apix.file.entity.FileStore;
import com.apix.file.service.FileStoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.*;

/**
 * 文件管理 API — 对标 Python: routers/file_record.py
 */
@RestController
@RequestMapping("/file/file")
public class FileController {

    private static final Logger log = LoggerFactory.getLogger(FileController.class);

    @Autowired
    private FileStoreService fileStoreService;

    @Value("${apix.file.store-dir:./data/files}")
    private String storeDir;

    /**
     * 上传文件（多文件）— 对标 Python: insert_file
     *
     * 流式写入磁盘，不加载到内存，同时持久化文件元数据到 MySQL。
     */
    @PostMapping("/insert_file")
    public R<List<Map<String, String>>> insertFile(
            @RequestParam String clientId,
            @RequestParam("files") List<MultipartFile> files) {

        log.info("[File] insert_file: client={}, files={}", clientId, files.size());
        List<Map<String, String>> result = new ArrayList<>();

        for (MultipartFile file : files) {
            try {
                String fileId = UUID.randomUUID().toString().replace("-", "");
                String originalName = file.getOriginalFilename();
                if (originalName == null)
                    originalName = "unknown";

                // 存储路径: storeDir/clientId/fileId/originalName
                Path targetDir = Paths.get(storeDir, clientId, fileId);
                Files.createDirectories(targetDir);
                Path targetFile = targetDir.resolve(originalName);

                // 流式写入（不加载到内存）
                try (InputStream is = file.getInputStream()) {
                    Files.copy(is, targetFile, StandardCopyOption.REPLACE_EXISTING);
                }

                // 计算 SHA256
                String sha256 = computeSha256(targetFile);

                // 持久化到 MySQL
                fileStoreService.saveFile(
                        originalName,
                        targetFile.toString(),
                        file.getSize(),
                        file.getContentType() != null ? file.getContentType() : "unknown",
                        clientId,
                        sha256);

                Map<String, String> fileInfo = new LinkedHashMap<>();
                fileInfo.put("file_id", fileId);
                fileInfo.put("file_name", originalName);
                fileInfo.put("file_size", String.valueOf(file.getSize()));
                fileInfo.put("mime_type", file.getContentType() != null ? file.getContentType() : "unknown");
                fileInfo.put("sha256", sha256);
                result.add(fileInfo);

                log.info("[File] Saved file: {} ({} bytes, id={})", originalName, file.getSize(), fileId);

            } catch (IOException e) {
                log.error("[File] Failed to save file: {}", file.getOriginalFilename(), e);
                Map<String, String> errorInfo = new LinkedHashMap<>();
                errorInfo.put("file_name", file.getOriginalFilename());
                errorInfo.put("error", e.getMessage());
                result.add(errorInfo);
            }
        }

        return R.ok(result);
    }

    /**
     * 获取最近文件列表。
     */
    @PostMapping("/get_recent_files")
    public R<List<Map<String, Object>>> getRecentFiles(@RequestBody Map<String, Object> payload) {
        String clientId = (String) payload.get("client_id");
        int limit = payload.containsKey("limit") ? (int) payload.get("limit") : 5;

        log.info("[File] get_recent_files: client={}, limit={}", clientId, limit);

        try {
            Path clientDir = Paths.get(storeDir, clientId);
            if (!Files.exists(clientDir)) {
                return R.ok(Collections.emptyList());
            }

            List<Map<String, Object>> files = new ArrayList<>();
            Files.list(clientDir)
                    .sorted((a, b) -> {
                        try {
                            return Files.getLastModifiedTime(b).compareTo(Files.getLastModifiedTime(a));
                        } catch (IOException e) {
                            return 0;
                        }
                    })
                    .limit(limit)
                    .forEach(dir -> {
                        try {
                            String fileId = dir.getFileName().toString();
                            Files.list(dir).forEach(file -> {
                                try {
                                    Map<String, Object> info = new LinkedHashMap<>();
                                    info.put("file_id", fileId);
                                    info.put("file_name", file.getFileName().toString());
                                    info.put("file_size", Files.size(file));
                                    info.put("upload_at", Files.getLastModifiedTime(file).toString());
                                    files.add(info);
                                } catch (IOException ignored) {
                                }
                            });
                        } catch (IOException ignored) {
                        }
                    });

            return R.ok(files);

        } catch (IOException e) {
            log.error("[File] get_recent_files failed", e);
            return R.error(500, e.getMessage());
        }
    }

    /**
     * 更新文件状态（软删除）。
     */
    @PostMapping("/update_file")
    public R<String> updateFile(@RequestBody Map<String, Object> payload) {
        String fileId = (String) payload.get("file_id");
        boolean isDeleted = payload.containsKey("is_deleted") && (boolean) payload.get("is_deleted");

        log.info("[File] update_file: id={}, deleted={}", fileId, isDeleted);
        if (isDeleted) {
            fileStoreService.softDelete(fileId);
        }

        return R.ok("ok");
    }

    /**
     * 计算文件 SHA256。
     */
    private String computeSha256(Path filePath) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int bytesRead;
            try (InputStream is = Files.newInputStream(filePath)) {
                while ((bytesRead = is.read(buffer)) != -1) {
                    digest.update(buffer, 0, bytesRead);
                }
            }
            byte[] hash = digest.digest();
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();
        } catch (Exception e) {
            log.warn("[File] SHA256 computation failed", e);
            return "";
        }
    }
}
