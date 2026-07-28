package com.hewei.hzyjy.xunzhi.interview.api.io.resp;

import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * 复习条目响应DTO。
 */
@Data
public class ReviewItemRespDTO {

    private Long id;

    /**
     * 来源面试会话
     */
    private String sessionId;

    /**
     * 弱项知识点
     */
    private String knowledgePoint;

    /**
     * 严重度：1=轻微，2=一般，3=严重
     */
    private Integer severity;

    /**
     * 复习建议
     */
    private String suggestion;

    /**
     * 知识库关联片段（可空）
     */
    private List<ReviewKbRefDTO> kbRefs;

    /**
     * 状态：0=待复习，1=已掌握
     */
    private Integer status;

    private Date createTime;

    private Date updateTime;
}
