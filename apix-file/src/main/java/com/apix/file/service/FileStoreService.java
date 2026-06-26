package com.apix.file.service;

import com.apix.file.entity.FileStore;
import com.apix.file.mapper.FileStoreMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 文件存储服务 — 管理文件元数据的 MySQL 持久化
 */
@Service
public class FileStoreService {

    private static final Logger log = LoggerFactory.getLogger(FileStoreService.class);

    @Autowired
    private FileStoreMapper fileStoreMapper;

    /**
     * 保存文件记录。
     */
    public FileStore saveFile(String fileName, String filePath, long fileSize,
            String mimeType, String userUid, String sha256) {
        FileStore record = new FileStore();
        record.setFileId(UUID.randomUUID().toString().replace("-", ""));
        record.setFileName(fileName);
        record.setFilePath(filePath);
        record.setFileSize(fileSize);
        record.setMimeType(mimeType);
        record.setUserUid(userUid);
        record.setSha256(sha256);
        record.setDeleted(false);
        record.setUploadAt(LocalDateTime.now());

        fileStoreMapper.insert(record);
        log.info("[FileStore] Saved record: id={}, name={}", record.getFileId(), fileName);
        return record;
    }

    /**
     * 软删除文件记录。
     */
    public void softDelete(String fileId) {
        FileStore record = fileStoreMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<FileStore>()
                        .eq(FileStore::getFileId, fileId));
        if (record != null) {
            record.setDeleted(true);
            record.setDeletedAt(LocalDateTime.now());
            fileStoreMapper.updateById(record);
            log.info("[FileStore] Soft deleted: id={}", fileId);
        }
    }

    /**
     * 查询最近文件列表。
     */
    public List<FileStore> getRecentFiles(String userUid, int limit) {
        return fileStoreMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<FileStore>()
                        .eq(FileStore::getUserUid, userUid)
                        .eq(FileStore::getDeleted, false)
                        .orderByDesc(FileStore::getUploadAt)
                        .last("LIMIT " + limit));
    }

    /**
     * 根据 fileId 查询文件。
     */
    public FileStore getByFileId(String fileId) {
        return fileStoreMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<FileStore>()
                        .eq(FileStore::getFileId, fileId));
    }
}
