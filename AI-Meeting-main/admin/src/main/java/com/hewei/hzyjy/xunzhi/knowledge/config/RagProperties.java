package com.hewei.hzyjy.xunzhi.knowledge.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * RAG 链路配置（检索规则引擎 + 精排 + Prompt 模板）。
 */
@Data
@Component
@ConfigurationProperties(prefix = "xunzhi-agent.rag")
public class RagProperties {

    /**
     * 向量存储引擎：elasticsearch（默认） | milvus
     */
    private String vectorStore = "elasticsearch";

    private RuleEngine ruleEngine = new RuleEngine();

    private Rerank rerank = new Rerank();

    private Prompt prompt = new Prompt();

    @Data
    public static class RuleEngine {

        private Boolean enable = true;

        private String defaultChainId = "default_rag_chain";

        private Boolean failOpen = true;

        private Integer defaultTopK = 5;

        private Integer defaultRerankTopN = 3;

        private Boolean enableWebSearch = true;

        private Boolean enableQueryRewrite = true;

        private Integer queryRewriteTimeoutMs = 2500;

        private String queryRewriteModel = "qwen-turbo";

        /**
         * CRAG 合格阈值（仅对 DashScope relevance_score 有效）
         */
        private Double gradePassThreshold = 0.5;

        private Boolean enableLlmGrader = false;
    }

    @Data
    public static class Rerank {

        /**
         * dashscope（阿里云百炼） | cosine（降级实现）
         */
        private String provider = "dashscope";

        private String baseUrl = "https://dashscope.aliyuncs.com/api/v1/services/rerank/text-rerank/text-rerank";

        private String apiKey = "";

        private String model = "gte-rerank-v2";

        private Integer timeoutMs = 1500;
    }

    @Data
    public static class Prompt {

        /**
         * RAG 增强模板，占位符 {context}、{question}；库级 prompt_template 非空时优先
         */
        private String ragTemplate = """
                你是一位知识渊博的AI助手，请基于以下参考资料回答用户问题。
                回答要求：
                1. 优先使用参考资料中的信息
                2. 如果参考资料不足以回答，请明确说明并借鉴你的知识
                3. 引用参考资料时请在句末标注来源编号，如 [1]、[2]

                【参考资料】
                {context}

                【用户问题】
                {question}""";

        /**
         * 知识库对话系统提示词
         */
        private String systemPrompt = "你是讯智AI助手，请根据对话历史和参考资料，提供准确、有用的回答。";
    }
}
