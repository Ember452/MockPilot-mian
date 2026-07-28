CREATE TABLE IF NOT EXISTS review_item (
    id              BIGINT(20)    NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_id         BIGINT(20)    NOT NULL COMMENT '用户ID',
    session_id      VARCHAR(64)   NOT NULL DEFAULT '' COMMENT '来源面试会话',
    knowledge_point VARCHAR(128)  NOT NULL DEFAULT '' COMMENT '弱项知识点',
    severity        TINYINT(4)    NOT NULL DEFAULT 2 COMMENT '严重度: 1=轻微, 2=一般, 3=严重',
    suggestion      VARCHAR(1024) NULL COMMENT '复习建议',
    kb_refs_json    TEXT          NULL COMMENT '知识库关联片段引用JSON（可空）',
    status          TINYINT(4)    NOT NULL DEFAULT 0 COMMENT '状态: 0=待复习, 1=已掌握',
    create_time     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    del_flag        TINYINT(1)    NOT NULL DEFAULT 0 COMMENT '删除标识: 0=正常, 1=删除',
    PRIMARY KEY (id),
    KEY idx_user_point (user_id, knowledge_point),
    UNIQUE KEY uk_session_point (session_id, knowledge_point)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='复习清单条目表';
