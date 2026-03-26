package com.hewei.hzyjy.xunzhi.common.enums;

import lombok.Getter;

/**
 * 表情类型枚举
 * 根据讯飞表情识别接口返回的label值映射对应的表情类型
 */
@Getter
public enum ExpressionType {
    
    NEUTRAL(0, "中性"),
    HAPPY(1, "高兴"),
    SAD(2, "伤心"),
    ANGRY(3, "生气"),
    SURPRISED(4, "惊讶"),
    SCARED(5, "害怕"),
    DISGUSTED(6, "厌恶"),
    CONFUSED(7, "困惑");
    
    private final Integer code;
    private final String description;
    
    ExpressionType(Integer code, String description) {
        this.code = code;
        this.description = description;
    }
    
    /**
     * 根据code获取表情类型
     * 
     * @param code 表情代码
     * @return 表情类型枚举
     */
    public static ExpressionType getByCode(Integer code) {
        if (code == null) {
            return NEUTRAL;
        }
        
        for (ExpressionType type : ExpressionType.values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        
        return NEUTRAL;
    }
}