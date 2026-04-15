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

    private String username;

    private Integer documentCount;

    private Integer chunkCount;

    private Integer isEnabled;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Integer delFlag;
}
