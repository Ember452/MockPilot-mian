package com.hewei.hzyjy.xunzhi.knowledge.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class ElasticsearchConfig {

    @Value("${elasticsearch.host:192.168.100.129}")
    private String host;

    @Value("${elasticsearch.port:9200}")
    private int port;

    @Bean
    public ElasticsearchClient elasticsearchClient() {
        ElasticsearchClient client = ElasticsearchClient.of(b -> b
                .host("http://" + host + ":" + port)
        );
        log.info("ElasticsearchClient initialized, host={}:{}", host, port);
        return client;
    }
}
