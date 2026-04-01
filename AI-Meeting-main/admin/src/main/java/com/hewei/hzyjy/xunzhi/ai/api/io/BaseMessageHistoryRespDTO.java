package com.hewei.hzyjy.xunzhi.ai.api.io;

import java.util.Date;

/**
 * 基础消息历史响应DTO接口
 * 定义通用的消息历史响应属性和方法
 */
public interface BaseMessageHistoryRespDTO {
    
    /**
     * 获取消息ID
     */
    String getId();
    
    /**
     * 设置消息ID
     */
    void setId(String id);
    
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
    Integer getMessageType();
    
    /**
     * 设置消息类型
     */
    void setMessageType(Integer messageType);
    
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
     * 获取Token消耗数量
     */
    Integer getTokenCount();
    
    /**
     * 设置Token消耗数量
     */
    void setTokenCount(Integer tokenCount);
    
    /**
     * 获取响应时间
     */
    Integer getResponseTime();
    
    /**
     * 设置响应时间
     */
    void setResponseTime(Integer responseTime);
    
    /**
     * 获取创建时间
     */
    Date getCreateTime();
    
    /**
     * 设置创建时间
     */
    void setCreateTime(Date createTime);
}