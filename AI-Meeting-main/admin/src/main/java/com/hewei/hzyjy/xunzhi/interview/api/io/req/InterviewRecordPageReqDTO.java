package com.hewei.hzyjy.xunzhi.interview.api.io.req;

import lombok.Data;

/**
 * 面试记录分页查询请求DTO
 */
@Data
public class InterviewRecordPageReqDTO {

    /**
     * 当前页码
     */
    private Integer pageNum = 1;

    /**
     * 每页数量
     */
    private Integer pageSize = 10;

    /**
     * 会话ID（可选，用于筛选）
     */
    private String sessionId;

    /**
     * 最低分数（可选，用于筛选）
     */
    private Integer minScore;

    /**
     * 最高分数（可选，用于筛选）
     */
    private Integer maxScore;

    /**
     * 面试方向（可选，用于筛选）
     */
    private String interviewDirection;
}