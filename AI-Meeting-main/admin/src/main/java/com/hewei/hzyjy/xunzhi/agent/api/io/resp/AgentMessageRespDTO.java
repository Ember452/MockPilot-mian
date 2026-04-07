package com.hewei.hzyjy.xunzhi.agent.api.io.resp;

import lombok.Data;

@Data
public class AgentMessageRespDTO {

    private String sessionId;

    private String userName;

    private String userMessage;

    private String chatMessage;

    private Integer messageSeq;

    private Integer tokenCount;

    private Integer responseTime;

    private Integer isSuccess = 1;

    private String errorMessage;
}
