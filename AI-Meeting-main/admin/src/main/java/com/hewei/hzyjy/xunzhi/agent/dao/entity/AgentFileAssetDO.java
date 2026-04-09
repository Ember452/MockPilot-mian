package com.hewei.hzyjy.xunzhi.agent.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 文件上传记录
 */
@Data
@TableName("agent_file_asset")
public class AgentFileAssetDO {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 智能体ID
     */
    private Long agentId;

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 上传用户名
     */
    private String userName;

    /**
     * 业务类型
     */
    private String bizType;

    /**
     * 存储平台
     */
    private String sourcePlatform;

    /**
     * 原始文件名
     */
    private String fileName;

    /**
     * 文件扩展名
     */
    private String fileExt;

    /**
     * 文件MIME类型
     */
    private String contentType;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * 文件URL
     */
    private String fileUrl;

    /**
     * 上传状态：1 成功，2 失败
     */
    private Integer uploadStatus;

    /**
     * 备注
     */
    private String remark;

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

