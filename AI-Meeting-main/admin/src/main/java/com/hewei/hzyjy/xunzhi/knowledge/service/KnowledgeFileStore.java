package com.hewei.hzyjy.xunzhi.knowledge.service;

import com.hewei.hzyjy.xunzhi.common.config.storage.ApplicationStorageProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * 知识库原始文件留存：{knowledgeDocDir}/{kbId}/{docId}.{ext}。
 * 留存是索引重建的前置能力，全部操作 fail-open——失败仅告警，不阻断主流程。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KnowledgeFileStore {

    private final ApplicationStorageProperties storageProperties;

    /**
     * 落盘并返回相对路径 {kbId}/{docId}.{ext}；失败返回 null。
     */
    public String save(Long kbId, String docId, String fileType, byte[] fileBytes) {
        String relativePath = kbId + "/" + docId + "." + fileType;
        try {
            Path target = resolve(relativePath);
            Files.createDirectories(target.getParent());
            Files.write(target, fileBytes);
            return relativePath;
        } catch (Exception e) {
            log.warn("Failed to persist knowledge doc file: {}", relativePath, e);
            return null;
        }
    }

    /**
     * 读取留存文件，路径为空/文件不存在/读取失败均返回 null。
     */
    public byte[] read(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return null;
        }
        try {
            Path target = resolve(relativePath);
            if (!Files.exists(target)) {
                return null;
            }
            return Files.readAllBytes(target);
        } catch (Exception e) {
            log.warn("Failed to read knowledge doc file: {}", relativePath, e);
            return null;
        }
    }

    public void delete(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return;
        }
        try {
            Files.deleteIfExists(resolve(relativePath));
        } catch (Exception e) {
            log.warn("Failed to delete knowledge doc file: {}", relativePath, e);
        }
    }

    /**
     * 删除知识库时级联删除 {kbId}/ 整个目录。
     */
    public void deleteKbDir(Long kbId) {
        Path dir = storageProperties.getKnowledgeDocPath().resolve(String.valueOf(kbId)).normalize();
        if (!Files.exists(dir)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(dir)) {
            paths.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (Exception e) {
                    log.warn("Failed to delete knowledge doc path: {}", p, e);
                }
            });
        } catch (Exception e) {
            log.warn("Failed to delete knowledge doc dir for kbId={}", kbId, e);
        }
    }

    private Path resolve(String relativePath) {
        return storageProperties.getKnowledgeDocPath().resolve(relativePath).normalize();
    }
}
