package com.hewei.hzyjy.xunzhi.common.util;

import com.hewei.hzyjy.xunzhi.common.convention.exception.ClientException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * 文件上传工具类
 * 提供文件类型验证、大小限制等功能
 */
public class FileUploadUtil {

    /**
     * 支持的文件类型枚举
     */
    public enum FileType {
        PDF("application/pdf", Arrays.asList(".pdf"), 20 * 1024 * 1024), // 20MB
        IMAGE("image/", Arrays.asList(".jpg", ".jpeg", ".png", ".gif", ".bmp"), 10 * 1024 * 1024), // 10MB
        AUDIO("audio/", Arrays.asList(".mp3", ".wav", ".pcm", ".m4a"), 50 * 1024 * 1024), // 50MB
        VIDEO("video/", Arrays.asList(".mp4", ".avi", ".mov", ".wmv"), 100 * 1024 * 1024); // 100MB

        private final String mimeTypePrefix;
        private final List<String> extensions;
        private final long maxSize;

        FileType(String mimeTypePrefix, List<String> extensions, long maxSize) {
            this.mimeTypePrefix = mimeTypePrefix;
            this.extensions = extensions;
            this.maxSize = maxSize;
        }

        public String getMimeTypePrefix() {
            return mimeTypePrefix;
        }

        public List<String> getExtensions() {
            return extensions;
        }

        public long getMaxSize() {
            return maxSize;
        }
    }

    /**
     * 获取文件扩展名
     * 
     * @param filename 文件名
     * @return 文件扩展名（包含点号）
     */
    private static String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return "";
        }
        return filename.substring(lastDotIndex);
    }

}