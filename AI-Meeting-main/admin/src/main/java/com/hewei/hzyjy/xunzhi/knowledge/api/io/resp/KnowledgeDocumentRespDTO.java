package com.hewei.hzyjy.xunzhi.knowledge.api.io.resp;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class KnowledgeDocumentRespDTO {
    private String id;
    private String docId;
    private String fileName;
    private String fileType;
    private Long fileSize;
    private Integer status;
    private Integer chunkCount;
    private LocalDateTime createTime;
}
