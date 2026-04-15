package com.hewei.hzyjy.xunzhi.knowledge.api.io.req;

import lombok.Data;

@Data
public class KnowledgeChatReqDTO {
    private String sessionId;
    private String inputMessage;
    private Long kbId;
    private Long aiId;
    private String userName;
}
