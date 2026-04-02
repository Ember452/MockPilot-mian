package com.hewei.hzyjy.xunzhi.ai.api.io.resp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI模型选项响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiModelOptionRespDTO {

    /**
     * AI配置ID
     */
    private Long id;

    /**
     * AI名称（如：GPT-4, 通义千问）
     */
    private String aiName;
    
    /**
     * AI类型
     */
    private Integer aiType;
}
