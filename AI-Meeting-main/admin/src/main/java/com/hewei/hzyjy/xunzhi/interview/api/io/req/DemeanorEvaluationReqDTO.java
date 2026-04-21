package com.hewei.hzyjy.xunzhi.interview.api.io.req;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

/**
 * 神态评估请求DTO
 */
@Data
public class DemeanorEvaluationReqDTO {
    
    /**
     * 用户名
     */
    private String userName;
    
    /**
     * 智能体ID
     */
    private Long agentId;
    
    /**
     * 会话ID
     */
    private String sessionId;
    
    /**
     * 用户照片
     */
    private MultipartFile userPhoto;
}