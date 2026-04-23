package com.hewei.hzyjy.xunzhi.interview.api.io.resp;

import lombok.Data;

/**
 * 雷达图数据传输对象
 */
@Data
public class RadarChartDTO {
    
    /**
     * 简历评估得分 (0-100)
     */
    private Integer resumeScore;
    
    /**
     * 面试表现得分 (0-100)
     */
    private Integer interviewPerformance;
    
    /**
     * 神态管理评分 (0-100)
     */
    private Integer demeanorEvaluation;
    
    /**
     * 用户潜力指数 (0-100)
     */
    private Integer potentialIndex;
    
    /**
     * 专业技能评分 (0-100)
     */
    private Integer professionalSkills;
}