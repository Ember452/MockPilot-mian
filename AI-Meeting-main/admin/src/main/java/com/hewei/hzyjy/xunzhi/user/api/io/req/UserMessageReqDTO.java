package com.hewei.hzyjy.xunzhi.user.api.io.req;

import lombok.Data;

@Data
public class UserMessageReqDTO {

    private String userName;

    private String inputMessage;

    private int messageSeq;

    private String sessionId;
}
