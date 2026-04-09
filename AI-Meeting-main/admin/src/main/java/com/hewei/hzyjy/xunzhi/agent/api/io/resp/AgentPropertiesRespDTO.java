package com.hewei.hzyjy.xunzhi.agent.api.io.resp;

import lombok.Data;
import java.util.List;

@Data
public class AgentPropertiesRespDTO {

    private Long id;

    private String agentName;

    private String apiSecret;

    private String apiKey;

    private String apiFlowId;

    /**
     * 标签信息列表
     */
    private List<TagInfo> tags;

    @Data
    public static class TagInfo {
        /**
         * 标签代码
         */
        private Integer code;
        
        /**
         * 标签名称
         */
        private String name;
        
        /**
         * 标签颜色
         */
        private String color;
    }
}