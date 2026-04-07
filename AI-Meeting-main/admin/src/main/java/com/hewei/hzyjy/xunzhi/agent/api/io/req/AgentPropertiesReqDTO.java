package com.hewei.hzyjy.xunzhi.agent.api.io.req;

import lombok.Data;
import java.util.List;

@Data
public class AgentPropertiesReqDTO {

    private Long id;

    private String agentName;

    private String apiSecret;

    private String apiKey;

    private String apiFlowId;

    /**
     * 标签代码列表，对应AgentTagType枚举的code值
     */
    private List<Integer> tagCodes;

    /**
     * 当前页码
     */
    private Integer pageNum ;

    /**
     * 每页数量
     */
    private Integer pageSize;
}