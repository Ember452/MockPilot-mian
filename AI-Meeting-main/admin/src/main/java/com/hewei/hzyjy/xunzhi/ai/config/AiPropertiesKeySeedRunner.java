package com.hewei.hzyjy.xunzhi.ai.config;

import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * ai_properties 密钥占位符注入：seed SQL 中仅保留占位符（避免真实密钥进 Git），
 * 启动时若 .env 配置了对应环境变量，则把仍为占位符的行更新为真实密钥，
 * 使克隆者只需填写 .env 即可开箱使用 AI 聊天。已被用户改过的行（非占位符）不覆盖。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiPropertiesKeySeedRunner implements ApplicationRunner {

    private static final String DEEPSEEK_PLACEHOLDER = "sk-your-deepseek-api-key";
    private static final String DEEPSEEK_ENV_KEY = "DEEPSEEK_API_KEY";

    private final JdbcTemplate jdbcTemplate;
    private final Environment environment;

    @Override
    public void run(ApplicationArguments args) {
        seedKey(DEEPSEEK_ENV_KEY, DEEPSEEK_PLACEHOLDER);
    }

    private void seedKey(String envKey, String placeholder) {
        String apiKey = environment.getProperty(envKey);
        if (StrUtil.isBlank(apiKey) || placeholder.equals(apiKey)) {
            return;
        }
        try {
            int updated = jdbcTemplate.update(
                    "UPDATE ai_properties SET api_key = ? WHERE api_key = ?", apiKey, placeholder);
            if (updated > 0) {
                log.info("Seeded api_key from env {} into {} ai_properties row(s)", envKey, updated);
            }
        } catch (Exception e) {
            log.warn("Seeding api_key from env {} failed (non-blocking): {}", envKey, e.getMessage());
        }
    }
}
