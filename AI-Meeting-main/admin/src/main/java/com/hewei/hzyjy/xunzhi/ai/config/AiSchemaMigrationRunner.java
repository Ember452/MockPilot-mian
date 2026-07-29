package com.hewei.hzyjy.xunzhi.ai.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * user_model_preference 表结构自动迁移：存量库（docker 数据卷非空时 initdb 脚本不再执行）
 * 缺表时启动补建，失败仅告警不阻断启动。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiSchemaMigrationRunner implements ApplicationRunner {

    private static final String CREATE_USER_MODEL_PREFERENCE_SQL = """
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
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户功能级默认模型绑定表'
            """;

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        try {
            jdbcTemplate.execute(CREATE_USER_MODEL_PREFERENCE_SQL);
        } catch (Exception e) {
            log.warn("Schema migration for user_model_preference failed (non-blocking): {}", e.getMessage());
        }
    }
}
