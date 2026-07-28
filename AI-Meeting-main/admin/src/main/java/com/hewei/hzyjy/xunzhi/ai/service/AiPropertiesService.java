package com.hewei.hzyjy.xunzhi.ai.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.hewei.hzyjy.xunzhi.ai.dao.entity.AiPropertiesDO;
import com.hewei.hzyjy.xunzhi.ai.api.io.req.AiPropertiesCreateReqDTO;
import com.hewei.hzyjy.xunzhi.ai.api.io.req.AiPropertiesPageReqDTO;
import com.hewei.hzyjy.xunzhi.ai.api.io.req.AiPropertiesUpdateReqDTO;
import com.hewei.hzyjy.xunzhi.ai.api.io.resp.AiPropertiesRespDTO;

import com.hewei.hzyjy.xunzhi.ai.api.io.resp.AiModelOptionRespDTO;

import java.util.List;

public interface AiPropertiesService extends IService<AiPropertiesDO> {

    void createAiProperties(AiPropertiesCreateReqDTO requestParam, String username);

    void updateAiProperties(AiPropertiesUpdateReqDTO requestParam, String username);

    void deleteAiProperties(Long id, String username);

    AiPropertiesRespDTO getAiPropertiesById(Long id, String username);

    IPage<AiPropertiesRespDTO> pageAiProperties(AiPropertiesPageReqDTO requestParam, String username);

    List<AiPropertiesRespDTO> listEnabledAiProperties(String username);

    void toggleAiPropertiesStatus(Long id, Integer isEnabled, String username);

    List<AiPropertiesRespDTO> getAllEnabledAiProperties(String username);

    AiPropertiesDO getEnabledByAiType(String aiType);

    AiPropertiesDO getDefaultDoubaoConfig();

    /**
     * 获取所有可用AI模型（用于前端下拉列表）
     */
    List<AiModelOptionRespDTO> getAvailableAiModels(String username);

    /**
     * 按 aiId 加载可用配置并校验归属（公共或本人），不满足则抛异常
     */
    AiPropertiesDO getUsableById(Long aiId, String username);

    /**
     * 测试连接：按请求体中的配置发一次最小 /chat/completions 请求
     */
    String testConnection(AiPropertiesCreateReqDTO requestParam);
}

