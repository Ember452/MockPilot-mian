package com.hewei.hzyjy.xunzhi.agent.api.io.resp;

import lombok.Data;

import java.util.Date;

@Data
public class AgentFileUploadRespDTO {

    private Long id;

    private String sessionId;

    private String bizType;

    private String fileName;

    private Long fileSize;

    private String contentType;

    private String fileUrl;

    private Date createTime;
}
