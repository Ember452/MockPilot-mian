package com.hewei.hzyjy.xunzhi.knowledge.dao.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Data
@Document(collection = "knowledge_documents")
public class KnowledgeDocument {

    @Id
    private String id;

    @Field("kb_id")
    private Long kbId;

    @Field("doc_id")
    private String docId;

    @Field("file_name")
    private String fileName;

    @Field("file_type")
    private String fileType;

    @Field("file_size")
    private Long fileSize;

    @Field("username")
    private String username;

    @Field("status")
    private Integer status;

    @Field("chunk_count")
    private Integer chunkCount;

    @Field("create_time")
    private LocalDateTime createTime;

    @Field("update_time")
    private LocalDateTime updateTime;
}
