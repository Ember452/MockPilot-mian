package com.hewei.hzyjy.xunzhi.interview.api.io.resp;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * 表情识别结果DTO
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ExpressionRecognitionResult {
    
    /**
     * 识别是否成功
     */
    private Boolean success;
    
    /**
     * 表情类型
     */
    private String expressionType;
    
    /**
     * 置信度
     */
    private Double confidence;
    
    /**
     * 响应消息
     */
    private String message;
    
    /**
     * 文件名
     */
    private String fileName;
    
    /**
     * 请求ID
     */
    private String requestId;

    public boolean isSuccess() {
        return success != null && success;
    }
}