package com.hewei.hzyjy.xunzhi.agent.service;

import com.hewei.hzyjy.xunzhi.common.convention.result.PageInfo;
import com.hewei.hzyjy.xunzhi.agent.dao.entity.AgentPropertiesDO;
import com.baomidou.mybatisplus.extension.service.IService;
import com.hewei.hzyjy.xunzhi.agent.api.io.req.AgentPropertiesReqDTO;
import com.hewei.hzyjy.xunzhi.agent.api.io.resp.AgentPropertiesRespDTO;

import java.util.List;

/**
* @author 20866
* @description 针对表【agent_properties】的数据库操作Service
* @createDate 2025-05-27 10:08:58
*/
public interface AgentPropertiesService extends IService<AgentPropertiesDO> {
    /**
     * 创建智能体配置
     */
    void create(AgentPropertiesReqDTO requestParam);

    /**
     * 删除智能体配置
     */
    void delete(Long id);

    /**
     * 更新智能体配置
     */
    void update(AgentPropertiesReqDTO requestParam);

    /**
     * 根据名称查询智能体配置
     */
    AgentPropertiesRespDTO getByName(String name);

    /**
     * 分页查询智能体配置
     */
    PageInfo<AgentPropertiesRespDTO> getByPage(AgentPropertiesReqDTO requestParam);

    /**
     * 查询前10个智能体配置
     */
    List<AgentPropertiesDO> listTop10();

    List<AgentPropertiesDO> listActiveAgents();
}

