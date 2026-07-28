package com.hewei.hzyjy.xunzhi.ai.api.io.resp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户功能级默认模型绑定响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserModelPreferenceRespDTO {

    /**
     * 功能编码：chat=AI对话, kb_chat=知识库对话, review=复习生成
     */
    private String featureCode;

    /**
     * 绑定的AI配置ID
     */
    private Long aiId;

    /**
     * 绑定的AI名称（配置已删除/停用时为空）
     */
    private String aiName;
}
