package com.hewei.hzyjy.xunzhi.common.config.database;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

/**
 * MongoDB配置类
 * 启用MongoDB审计功能，支持@CreatedDate和@LastModifiedDate注解
 */
@Configuration
@EnableMongoAuditing
public class MongoConfig {
}