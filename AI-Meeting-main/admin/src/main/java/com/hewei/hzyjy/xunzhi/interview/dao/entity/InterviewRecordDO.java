package com.hewei.hzyjy.xunzhi.interview.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 面试记录实体
 */
@Data
@TableName("interview_record")
public class InterviewRecordDO {

    /**
     * ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 会话ID
     */
    @TableField("session_id")
    private String sessionId;

    /**
     * 面试得分
     */
    @TableField("interview_score")
    private Integer interviewScore;

    /**
     * 简历得分
     */
    @TableField("resume_score")
    private Integer resumeScore;

    /**
     * 面试状态（INIT/IN_PROGRESS/FINISHED/EVALUATED）
     */
    @TableField("interview_status")
    private String interviewStatus;

    /**
     * 面试问题总数
     */
    @TableField("question_count")
    private Integer questionCount;

    /**
     * 面试会话智能体ID
     */
    @TableField("interviewer_agent_id")
    private Long interviewerAgentId;

    /**
     * 面试建议
     */
    @TableField("interview_suggestions")
    private String interviewSuggestions;

    /**
     * 面试方向
     */
    @TableField("interview_direction")
    private String interviewDirection;

    /**
     * 会话开始时间
     */
    @TableField("start_time")
    private Date startTime;

    /**
     * 会话结束时间
     */
    @TableField("end_time")
    private Date endTime;

    /**
     * 会话持续时长（秒）
     */
    @TableField("duration_seconds")
    private Integer durationSeconds;

    /**
     * 会话快照（JSON）
     */
    @TableField("session_snapshot_json")
    private String sessionSnapshotJson;

    /**
     * 创建时间
     */
    @TableField("create_time")
    private Date createTime;

    /**
     * 修改时间
     */
    @TableField("update_time")
    private Date updateTime;

    /**
     * 删除标识 0：未删除 1：已删除
     */
    @TableField("del_flag")
    private Integer delFlag;
}
