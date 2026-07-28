package com.hewei.hzyjy.xunzhi.ai.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 用户功能级默认模型绑定实体类
 */
@Data
@TableName("user_model_preference")
public class UserModelPreferenceDO {

    /**
     * ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 功能编码：chat=AI对话, kb_chat=知识库对话, review=复习生成
     */
    private String featureCode;

    /**
     * 绑定的AI配置ID（ai_properties.id）
     */
    private Long aiId;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 修改时间
     */
    private Date updateTime;

    /**
     * 删除标识 0：未删除 1：已删除
     */
    private Integer delFlag;
}
