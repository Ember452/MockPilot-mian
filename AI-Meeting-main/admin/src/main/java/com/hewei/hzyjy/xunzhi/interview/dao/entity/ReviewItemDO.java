package com.hewei.hzyjy.xunzhi.interview.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 复习清单条目实体
 */
@Data
@TableName("review_item")
public class ReviewItemDO {

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
     * 来源面试会话
     */
    @TableField("session_id")
    private String sessionId;

    /**
     * 弱项知识点
     */
    @TableField("knowledge_point")
    private String knowledgePoint;

    /**
     * 严重度：1=轻微，2=一般，3=严重
     */
    @TableField("severity")
    private Integer severity;

    /**
     * 复习建议
     */
    @TableField("suggestion")
    private String suggestion;

    /**
     * 知识库关联片段引用JSON（可空）
     */
    @TableField("kb_refs_json")
    private String kbRefsJson;

    /**
     * 状态：0=待复习，1=已掌握
     */
    @TableField("status")
    private Integer status;

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
