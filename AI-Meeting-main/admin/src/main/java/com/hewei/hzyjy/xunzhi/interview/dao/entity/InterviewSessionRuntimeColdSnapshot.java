package com.hewei.hzyjy.xunzhi.interview.dao.entity;

import com.hewei.hzyjy.xunzhi.interview.api.io.req.DemeanorScoreDTO;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.Map;

/**
 * 定义面试会话低频材料快照持久化实体，
 * 用于保存题目、简历上下文、面试方向和评分材料等不需要每轮答题都重写的恢复数据。
 *
 * @author 程序员牛肉
 */
@Data
@Document(collection = "interview_session_runtime_cold_snapshot")
public class InterviewSessionRuntimeColdSnapshot {

    /**
     * Mongo 文档主键，用于唯一标识一条低频材料快照记录。
     */
    @Id
    private String id;

    /**
     * 面试会话唯一标识，用于按 session 维度定位冷快照。
     */
    @Indexed(unique = true)
    private String sessionId;

    /**
     * 当前会话所属用户 ID，用于标识这份材料快照的归属用户。
     */
    @Indexed
    private Long userId;

    /**
     * 材料快照版本号，每次冷数据发生变化后递增，用于区分不同材料版本。
     */
    private Long materialVersion;

    /**
     * 简历文件地址，用于恢复简历来源和后续报告展示。
     */
    private String resumeFileUrl;

    /**
     * 面试类型，如 backend、frontend 等，用于表示本场面试的大类。
     */
    private String interviewType;

    /**
     * 面试方向或岗位方向，用于恢复题目背景和报告中的岗位信息。
     */
    private String direction;

    /**
     * 主问题集合，通常按题号映射题目内容，是恢复当前题面和流程的基础材料。
     */
    private Map<String, String> questions;

    /**
     * 题目建议集合，通常按题号映射提示或建议内容，用于恢复补充材料。
     */
    private Map<String, String> suggestions;

    /**
     * 简历解析上下文，用于恢复出题、追问和报告所依赖的背景信息。
     */
    private Map<String, Object> resumeContext;

    /**
     * 简历评分结果，用于恢复与展示候选人简历评价。
     */
    private Integer resumeScore;

    /**
     * 神态评分结果，用于恢复候选人仪态表现分。
     */
    private Integer demeanorScore;

    /**
     * 神态评分明细，用于恢复评分细项和报告展示内容。
     */
    private DemeanorScoreDTO demeanorDetails;

    /**
     * 业务语义下的材料刷新时间，用于表示低频材料最近一次被持久化的时刻。
     */
    private Date materialUpdatedAt;

    /**
     * 冷快照文档创建时间。
     */
    @CreatedDate
    private Date createTime;

    /**
     * 冷快照文档最后更新时间。
     */
    @LastModifiedDate
    private Date updateTime;
}
