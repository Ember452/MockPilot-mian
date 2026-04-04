package com.hewei.hzyjy.xunzhi.ai.enums;

import lombok.Getter;

/**
 * AI模型类型枚举
 * 用于定义支持的AI模型及其默认配置
 */
@Getter
public enum AiPropritiesType {

    OPENAI(1, "openai", "OpenAI", "https://api.openai.com/v1"),
    DOUBAO(2, "doubao", "豆包", "https://ark.cn-beijing.volces.com/api/v3"),
    SPARK(3, "spark", "讯飞星火", "https://spark-api-open.xf-yun.com/v1"),
    DEEPSEEK(4, "deepseek", "DeepSeek", "https://dashscope.aliyuncs.com/compatible-mode/v1"),
    OTHER(99, "other", "其他", "");

    private final Integer code;
    private final String type;
    private final String desc;
    private final String defaultBaseUrl;

    AiPropritiesType(Integer code, String type, String desc, String defaultBaseUrl) {
        this.code = code;
        this.type = type;
        this.desc = desc;
        this.defaultBaseUrl = defaultBaseUrl;
    }
    
    /**
     * 根据code获取类型
     */
    public static AiPropritiesType getByCode(Integer code) {
        for (AiPropritiesType type : AiPropritiesType.values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return OTHER;
    }
    
    /**
     * 根据type字符串获取类型
     */
    public static AiPropritiesType getByType(String type) {
        if (type == null) {
            return OTHER;
        }
        for (AiPropritiesType t : AiPropritiesType.values()) {
            if (t.getType().equalsIgnoreCase(type)) {
                return t;
            }
        }
        // 兼容旧数据
        if ("generalv3.5".equalsIgnoreCase(type)) {
            return SPARK;
        }
        return OTHER;
    }

    /**
     * 检查是否支持该类型
     */
    public static boolean isSupported(String type) {
        return getByType(type) != OTHER;
    }
}
