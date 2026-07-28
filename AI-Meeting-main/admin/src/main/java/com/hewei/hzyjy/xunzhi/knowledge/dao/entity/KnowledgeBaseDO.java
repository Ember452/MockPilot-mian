package com.hewei.hzyjy.xunzhi.knowledge.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("knowledge_base")
public class KnowledgeBaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String description;

    /**
     * RAG 增强 Prompt 模板（占位符 {context}/{question}），空则用全局默认
     */
    private String promptTemplate;

    /**
     * 建库时绑定的 embedding 模型标识，空串为 legacy 库（首次新增文档时回填）
     */
    private String embeddingModel;

    /**
     * embedding 向量维度
     */
    private Integer embeddingDim;

    private String username;

    private Integer documentCount;

    private Integer chunkCount;

    private Integer isEnabled;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Integer delFlag;
}
