package com.hewei.hzyjy.xunzhi.common.database;

import java.time.LocalDateTime;

/**
 * 基础消息接口
 * 定义消息实体的通用属性和方法
 */
public interface BaseMessage {
    
    /**
     * 获取消息ID
     */
    Object getId();
    
    /**
     * 设置消息ID
     */
    void setId(Object id);
    
    /**
     * 获取会话ID
     */
    String getSessionId();
    
    /**
     * 设置会话ID
     */
    void setSessionId(String sessionId);
    
    /**
     * 获取消息类型
     */
    String getMessageType();
    
    /**
     * 设置消息类型
     */
    void setMessageType(String messageType);
    
    /**
     * 获取消息内容
     */
    String getMessageContent();
    
    /**
     * 设置消息内容
     */
    void setMessageContent(String messageContent);
    
    /**
     * 获取消息序号
     */
    Integer getMessageSeq();
    
    /**
     * 设置消息序号
     */
    void setMessageSeq(Integer messageSeq);
    
    /**
     * 获取父消息ID
     */
    String getParentMsgId();
    
    /**
     * 设置父消息ID
     */
    void setParentMsgId(String parentMsgId);
    
    /**
     * 获取Token数量
     */
    Integer getTokenCount();
    
    /**
     * 设置Token数量
     */
    void setTokenCount(Integer tokenCount);
    
    /**
     * 获取响应时间
     */
    Long getResponseTime();
    
    /**
     * 设置响应时间
     */
    void setResponseTime(Long responseTime);
    
    /**
     * 获取错误信息
     */
    String getErrorMessage();
    
    /**
     * 设置错误信息
     */
    void setErrorMessage(String errorMessage);
    
    /**
     * 获取创建时间
     */
    LocalDateTime getCreateTime();
    
    /**
     * 设置创建时间
     */
    void setCreateTime(LocalDateTime createTime);
    
    /**
     * 获取更新时间
     */
    LocalDateTime getUpdateTime();
    
    /**
     * 设置更新时间
     */
    void setUpdateTime(LocalDateTime updateTime);
    
    /**
     * 获取删除标志
     */
    Integer getDelFlag();
    
    /**
     * 设置删除标志
     */
    void setDelFlag(Integer delFlag);
}