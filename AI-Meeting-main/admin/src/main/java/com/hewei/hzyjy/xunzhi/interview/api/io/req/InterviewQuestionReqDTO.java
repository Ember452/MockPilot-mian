package com.hewei.hzyjy.xunzhi.interview.api.io.req;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

/**
 * 面试题抽取请求DTO
 * @author system
 */
@Data
public class InterviewQuestionReqDTO {

    /**
     * 用户名
     */
    private String userName;

    /**
     * AgentID
     */
    private Long agentId;

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 简历PDF文件
     */
    private MultipartFile resumePdf;

    /**
     * 简历文件URL（上传后的文件地址）
     */
    private String resumeFileUrl;

}