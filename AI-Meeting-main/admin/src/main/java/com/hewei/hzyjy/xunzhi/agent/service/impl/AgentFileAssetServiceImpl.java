package com.hewei.hzyjy.xunzhi.agent.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hewei.hzyjy.xunzhi.agent.api.io.resp.AgentFileUploadRespDTO;
import com.hewei.hzyjy.xunzhi.agent.application.AgentResolver;
import com.hewei.hzyjy.xunzhi.agent.application.BusinessAgentResolver;
import com.hewei.hzyjy.xunzhi.agent.application.BusinessAgentScene;
import com.hewei.hzyjy.xunzhi.agent.dao.entity.AgentFileAssetDO;
import com.hewei.hzyjy.xunzhi.agent.dao.entity.AgentPropertiesDO;
import com.hewei.hzyjy.xunzhi.agent.dao.mapper.AgentFileAssetMapper;
import com.hewei.hzyjy.xunzhi.agent.service.AgentFileAssetService;
import com.hewei.hzyjy.xunzhi.common.convention.exception.ClientException;
import com.hewei.hzyjy.xunzhi.common.enums.AgentErrorCodeEnum;
import com.hewei.hzyjy.xunzhi.toolkit.xunfei.XingChenAIClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Date;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentFileAssetServiceImpl extends ServiceImpl<AgentFileAssetMapper, AgentFileAssetDO>
        implements AgentFileAssetService {

    private static final String DEFAULT_BIZ_TYPE = "general";
    private static final String SOURCE_PLATFORM_XINGCHEN = "xingchen";

    private final XingChenAIClient xingChenAIClient;
    private final AgentResolver agentResolver;
    private final BusinessAgentResolver businessAgentResolver;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AgentFileUploadRespDTO uploadAndPersist(
            String sessionId,
            String bizType,
            String username,
            MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ClientException("uploaded file cannot be empty", AgentErrorCodeEnum.AGENT_SAVE_ERROR);
        }

        AgentPropertiesDO agentProperties = resolveAgentProperties(sessionId);
        if (StrUtil.isBlank(agentProperties.getApiKey()) || StrUtil.isBlank(agentProperties.getApiSecret())) {
            throw new ClientException("agent api credentials are missing", AgentErrorCodeEnum.AGENT_SAVE_ERROR);
        }

        String fileUrl;
        try {
            fileUrl = xingChenAIClient.uploadFile(file, agentProperties.getApiKey(), agentProperties.getApiSecret());
        } catch (Exception ex) {
            log.error("File upload to xingchen failed, agentId={}, fileName={}",
                    agentProperties.getId(), file.getOriginalFilename(), ex);
            throw new ClientException(
                    "upload file to xingchen failed: " + ex.getMessage(),
                    ex,
                    AgentErrorCodeEnum.AGENT_SAVE_ERROR
            );
        }

        String originalFileName = normalizeFileName(file.getOriginalFilename());
        Date now = new Date();
        AgentFileAssetDO fileAssetDO = new AgentFileAssetDO();
        fileAssetDO.setAgentId(agentProperties.getId());
        fileAssetDO.setSessionId(StrUtil.isBlank(sessionId) ? null : sessionId.trim());
        fileAssetDO.setUserName(StrUtil.blankToDefault(username, "unknown"));
        fileAssetDO.setBizType(StrUtil.blankToDefault(bizType, DEFAULT_BIZ_TYPE));
        fileAssetDO.setSourcePlatform(SOURCE_PLATFORM_XINGCHEN);
        fileAssetDO.setFileName(originalFileName);
        fileAssetDO.setFileExt(extractFileExt(originalFileName));
        fileAssetDO.setContentType(file.getContentType());
        fileAssetDO.setFileSize(file.getSize());
        fileAssetDO.setFileUrl(fileUrl);
        fileAssetDO.setUploadStatus(1);
        fileAssetDO.setCreateTime(now);
        fileAssetDO.setUpdateTime(now);
        fileAssetDO.setDelFlag(0);

        boolean saved = save(fileAssetDO);
        if (!saved) {
            throw new ClientException("persist uploaded file url failed", AgentErrorCodeEnum.AGENT_SAVE_ERROR);
        }

        AgentFileUploadRespDTO respDTO = new AgentFileUploadRespDTO();
        respDTO.setId(fileAssetDO.getId());
        respDTO.setSessionId(fileAssetDO.getSessionId());
        respDTO.setBizType(fileAssetDO.getBizType());
        respDTO.setFileName(fileAssetDO.getFileName());
        respDTO.setFileSize(fileAssetDO.getFileSize());
        respDTO.setContentType(fileAssetDO.getContentType());
        respDTO.setFileUrl(fileAssetDO.getFileUrl());
        respDTO.setCreateTime(fileAssetDO.getCreateTime());
        return respDTO;
    }

    private AgentPropertiesDO resolveAgentProperties(String sessionId) {
        if (StrUtil.isNotBlank(sessionId)) {
            AgentPropertiesDO boundAgent = agentResolver.resolveAgent(sessionId, null);
            if (boundAgent != null) {
                return boundAgent;
            }
        }
        return businessAgentResolver.resolveRequired(BusinessAgentScene.GENERAL_AGENT_CHAT);
    }

    private String normalizeFileName(String originalFileName) {
        if (StrUtil.isBlank(originalFileName)) {
            return "unknown_" + System.currentTimeMillis();
        }
        return originalFileName.trim();
    }

    private String extractFileExt(String fileName) {
        if (StrUtil.isBlank(fileName)) {
            return null;
        }
        int idx = fileName.lastIndexOf('.');
        if (idx < 0 || idx == fileName.length() - 1) {
            return null;
        }
        return fileName.substring(idx + 1).toLowerCase();
    }
}
