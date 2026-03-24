package com.hewei.hzyjy.xunzhi.common.enums;

import lombok.Getter;

/**
 * 智能体标签类型枚举
 * 用于分类不同领域的智能体
 */
@Getter
public enum AgentTagType {
    
    FRONTEND(1, "前端"),
    BACKEND(2, "后端"),
    IOT(3, "物联网"),
    TEST(4, "测试"),
    AI(5, "人工智能"),
    DATA(6, "数据分析"),
    MOBILE(7, "移动开发"),
    DEVOPS(8, "运维"),
    DESIGN(9, "设计"),
    PRODUCT(10, "产品"),
    OTHER(99, "其他");
    
    private final Integer code;
    private final String name;

    
    AgentTagType(Integer code, String name) {
        this.code = code;
        this.name = name;
    }
    
    /**
     * 根据code获取标签类型
     * @param code 标签代码
     * @return 标签类型
     */
    public static AgentTagType getByCode(Integer code) {
        for (AgentTagType tagType : AgentTagType.values()) {
            if (tagType.getCode().equals(code)) {
                return tagType;
            }
        }
        return OTHER;
    }
    
    /**
     * 根据名称获取标签类型
     * @param name 标签名称
     * @return 标签类型
     */
    public static AgentTagType getByName(String name) {
        for (AgentTagType tagType : AgentTagType.values()) {
            if (tagType.getName().equals(name)) {
                return tagType;
            }
        }
        return OTHER;
    }
}