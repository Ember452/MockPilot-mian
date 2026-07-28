package com.hewei.hzyjy.xunzhi.common.config.storage;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

/**
 * Runtime storage configuration for uploads, temporary audio files and logs.
 */
@Data
@Component
@ConfigurationProperties(prefix = "xunzhi-agent.storage")
public class ApplicationStorageProperties {

    private String baseDir;

    private String uploadTempDir;

    private String audioTempDir;

    private String logDir;

    private String knowledgeDocDir;

    public Path getBasePath() {
        return Path.of(baseDir).toAbsolutePath().normalize();
    }

    public Path getUploadTempPath() {
        return Path.of(uploadTempDir).toAbsolutePath().normalize();
    }

    public Path getAudioTempPath() {
        return Path.of(audioTempDir).toAbsolutePath().normalize();
    }

    public Path getLogPath() {
        return Path.of(logDir).toAbsolutePath().normalize();
    }

    public Path getKnowledgeDocPath() {
        String dir = (knowledgeDocDir == null || knowledgeDocDir.isBlank())
                ? baseDir + "/knowledge-docs" : knowledgeDocDir;
        return Path.of(dir).toAbsolutePath().normalize();
    }
}
