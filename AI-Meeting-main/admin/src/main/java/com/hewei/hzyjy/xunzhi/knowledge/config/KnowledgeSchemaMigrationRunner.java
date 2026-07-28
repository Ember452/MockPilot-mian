package com.hewei.hzyjy.xunzhi.knowledge.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * knowledge_base 表结构自动迁移：存量库（docker 数据卷非空时 initdb 脚本不再执行）
 * 缺少新增列时启动补齐，失败仅告警不阻断启动。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KnowledgeSchemaMigrationRunner implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        addColumnIfMissing("prompt_template",
                "ALTER TABLE knowledge_base ADD COLUMN prompt_template TEXT NULL "
                        + "COMMENT 'RAG增强Prompt模板，空则用全局默认'");
        addColumnIfMissing("embedding_model",
                "ALTER TABLE knowledge_base ADD COLUMN embedding_model VARCHAR(128) NOT NULL DEFAULT '' "
                        + "COMMENT '建库时绑定的embedding模型，空为legacy库'");
        addColumnIfMissing("embedding_dim",
                "ALTER TABLE knowledge_base ADD COLUMN embedding_dim INT NOT NULL DEFAULT 1536 "
                        + "COMMENT 'embedding向量维度'");
    }

    private void addColumnIfMissing(String columnName, String alterSql) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.COLUMNS "
                            + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'knowledge_base' "
                            + "AND COLUMN_NAME = ?",
                    Integer.class, columnName);
            if (count != null && count == 0) {
                jdbcTemplate.execute(alterSql);
                log.info("Schema migration: added knowledge_base.{}", columnName);
            }
        } catch (Exception e) {
            log.warn("Schema migration for knowledge_base.{} skipped: {}", columnName, e.getMessage());
        }
    }
}
