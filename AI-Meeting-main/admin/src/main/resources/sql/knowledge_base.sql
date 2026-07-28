CREATE TABLE IF NOT EXISTS knowledge_base (
    id              BIGINT(20)   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    name            VARCHAR(128) NOT NULL DEFAULT '' COMMENT '知识库名称',
    description     VARCHAR(512) NOT NULL DEFAULT '' COMMENT '知识库描述',
    prompt_template TEXT         NULL COMMENT 'RAG增强Prompt模板，空则用全局默认',
    embedding_model VARCHAR(128) NOT NULL DEFAULT '' COMMENT '建库时绑定的embedding模型，空为legacy库',
    embedding_dim   INT(11)      NOT NULL DEFAULT 1536 COMMENT 'embedding向量维度',
    username        VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '所属用户',
    document_count  INT(11)      NOT NULL DEFAULT 0 COMMENT '文档数量',
    chunk_count     INT(11)      NOT NULL DEFAULT 0 COMMENT '分块总数',
    is_enabled      TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '启用状态: 1=启用, 0=禁用',
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    del_flag        TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '删除标识: 0=正常, 1=删除',
    PRIMARY KEY (id),
    KEY idx_username (username),
    KEY idx_del_flag (del_flag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识库表';
