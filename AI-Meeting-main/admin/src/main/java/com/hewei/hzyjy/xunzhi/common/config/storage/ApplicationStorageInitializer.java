package com.hewei.hzyjy.xunzhi.common.config.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Prepares required runtime directories before the application starts serving traffic.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApplicationStorageInitializer implements ApplicationRunner {

    private final ApplicationStorageProperties storageProperties;

    @Override
    public void run(ApplicationArguments args) {
        createDirectory(storageProperties.getBasePath());
        createDirectory(storageProperties.getUploadTempPath());
        createDirectory(storageProperties.getAudioTempPath());
        createDirectory(storageProperties.getLogPath());
        log.info("Runtime storage ready, baseDir={}", storageProperties.getBasePath());
    }

    private void createDirectory(Path path) {
        try {
            Files.createDirectories(path);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to initialize runtime directory: " + path, ex);
        }
    }
}
