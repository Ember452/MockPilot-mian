package com.hewei.hzyjy.xunzhi.ai.api.io.req;

import com.hewei.hzyjy.xunzhi.ai.dao.entity.AiPropertiesDO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.Data;

/**
 * AI配置分页查询请求DTO
 * @author nageoffer
 */
@Data
public class AiPropertiesPageReqDTO extends Page<AiPropertiesDO> {
    
    /**
     * AI名称（模糊查询）
     */
    private String aiName;
    
    /**
     * AI类型
     */
    private String aiType;
    
    /**
     * 是否启用 0：禁用 1：启用
     */
    private Integer isEnabled;
}
