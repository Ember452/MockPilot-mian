package com.hewei.hzyjy.xunzhi.agent.api.io.req;

import lombok.Data;

@Data
public class AgentConversationPageReqDTO {

    private Integer current = 1;

    private Integer size = 10;

    private Integer status;

    private String keyword;
}
