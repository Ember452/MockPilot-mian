package com.hewei.hzyjy.xunzhi.knowledge.config;

import io.milvus.v2.client.ConnectConfig;
import io.milvus.v2.client.MilvusClientV2;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@ConditionalOnProperty(name = "xunzhi-agent.rag.vector-store", havingValue = "milvus")
public class MilvusConfig {

    @Value("${milvus.host:localhost}")
    private String host;

    @Value("${milvus.port:19530}")
    private int port;

    @Bean(destroyMethod = "close")
    public MilvusClientV2 milvusClientV2() {
        MilvusClientV2 client = new MilvusClientV2(ConnectConfig.builder()
                .uri("http://" + host + ":" + port)
                .build());
        log.info("MilvusClientV2 initialized, host={}:{}", host, port);
        return client;
    }
}
