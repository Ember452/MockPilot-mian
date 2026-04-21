package com.hewei.hzyjy.xunzhi.interview.api.io.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 保存面试记录请求参数。
 */
@Data
public class InterviewRecordSaveReqDTO {

    @NotBlank(message = "sessionId不能为空")
    @Size(max = 64, message = "sessionId长度不能超过64个字符")
    private String sessionId;

    @PositiveOrZero(message = "interviewScore不能为负数")
    private Integer interviewScore;

    @Size(max = 10000, message = "interviewSuggestions长度不能超过10000个字符")
    private String interviewSuggestions;

    @Size(max = 128, message = "interviewDirection长度不能超过128个字符")
    private String interviewDirection;

    @Size(max = 64, message = "username长度不能超过64个字符")
    private String username;
}
