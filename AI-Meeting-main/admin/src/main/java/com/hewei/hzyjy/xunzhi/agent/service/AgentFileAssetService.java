package com.hewei.hzyjy.xunzhi.agent.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hewei.hzyjy.xunzhi.agent.api.io.resp.AgentFileUploadRespDTO;
import com.hewei.hzyjy.xunzhi.agent.dao.entity.AgentFileAssetDO;
import org.springframework.web.multipart.MultipartFile;

public interface AgentFileAssetService extends IService<AgentFileAssetDO> {

    AgentFileUploadRespDTO uploadAndPersist(String sessionId, String bizType, String username, MultipartFile file);
}
