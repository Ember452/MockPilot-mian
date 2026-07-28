CREATE TABLE IF NOT EXISTS user_model_preference (
    id           BIGINT(20)   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    username     VARCHAR(256) NOT NULL COMMENT '用户名',
    feature_code VARCHAR(32)  NOT NULL COMMENT '功能编码: chat=AI对话, kb_chat=知识库对话, review=复习生成',
    ai_id        BIGINT(20)   NOT NULL COMMENT '绑定的AI配置ID(ai_properties.id)',
    create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    del_flag     TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '删除标识: 0=正常, 1=删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_feature (username, feature_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户功能级默认模型绑定表';
