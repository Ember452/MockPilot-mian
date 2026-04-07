package com.hewei.hzyjy.xunzhi.agent.api.io.resp;

import lombok.Data;

import java.util.Date;

@Data
public class AgentConversationRespDTO {

    private String sessionId;

    private String agentName;

    private String conversationTitle;

    private Integer messageCount;

    private Integer totalTokens;

    private Integer status;

    private Date createTime;

    private Date updateTime;
}
