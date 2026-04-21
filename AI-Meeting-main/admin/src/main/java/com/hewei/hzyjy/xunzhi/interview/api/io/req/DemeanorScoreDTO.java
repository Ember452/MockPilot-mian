package com.hewei.hzyjy.xunzhi.interview.api.io.req;

import lombok.Data;

/**
 * 神态评分数据传输对象
 */
@Data
public class DemeanorScoreDTO {

    /**
     * 慌乱度 (0-100)
     */
    private Integer panicLevel;
    
    /**
     * 严肃程度 (0-100)
     */
    private Integer seriousnessLevel;
    
    /**
     * 表情处理 (0-100)
     */
    private Integer emoticonHandling;
    
    /**
     * 综合得分 (0-100)
     */
    private Integer compositeScore;
}