package com.hewei.hzyjy.xunzhi.ai.api.io.req;

import lombok.Data;

/**
 * 保存用户功能级默认模型绑定请求参数
 */
@Data
public class UserModelPreferenceSaveReqDTO {

    /**
     * 功能编码：chat=AI对话, kb_chat=知识库对话, review=复习生成
     */
    private String featureCode;

    /**
     * 绑定的AI配置ID；为空表示清除绑定（恢复平台默认）
     */
    private Long aiId;
}
