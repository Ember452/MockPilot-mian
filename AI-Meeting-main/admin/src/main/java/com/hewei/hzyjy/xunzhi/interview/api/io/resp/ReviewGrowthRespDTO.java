package com.hewei.hzyjy.xunzhi.interview.api.io.resp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

/**
 * 成长曲线响应DTO：面试分数时间序 + 弱项知识点聚合。
 */
@Data
public class ReviewGrowthRespDTO {

    /**
     * 分数时间序（按面试开始时间正序）
     */
    private List<ScorePointDTO> scoreTrend;

    /**
     * 弱项知识点聚合（顽固弱项置顶）
     */
    private List<WeakPointAggDTO> weakPoints;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScorePointDTO {

        private String sessionId;

        private Integer interviewScore;

        private Integer resumeScore;

        private Date startTime;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WeakPointAggDTO {

        private String knowledgePoint;

        /**
         * 出现次数（跨面试场次）
         */
        private Integer occurrences;

        /**
         * 已掌握条目数
         */
        private Integer mastered;

        /**
         * 掌握率 0~1
         */
        private Double masteryRate;

        /**
         * 顽固弱项：出现次数 >= 2
         */
        private Boolean stubborn;
    }
}
