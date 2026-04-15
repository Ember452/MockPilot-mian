package com.hewei.hzyjy.xunzhi.knowledge.api.io.resp;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class KnowledgeBaseRespDTO {
    private Long id;
    private String name;
    private String description;
    private Integer documentCount;
    private Integer chunkCount;
    private LocalDateTime createTime;
}
