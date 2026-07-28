package com.hewei.hzyjy.xunzhi.ai.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hewei.hzyjy.xunzhi.ai.enums.AiPropritiesType;
import com.hewei.hzyjy.xunzhi.ai.service.AiPropertiesService;
import com.hewei.hzyjy.xunzhi.ai.service.ApiKeyCryptoService;
import com.hewei.hzyjy.xunzhi.common.convention.exception.ClientException;
import com.hewei.hzyjy.xunzhi.ai.dao.entity.AiPropertiesDO;
import com.hewei.hzyjy.xunzhi.ai.dao.mapper.AiPropertiesMapper;
import com.hewei.hzyjy.xunzhi.ai.api.io.req.AiPropertiesCreateReqDTO;
import com.hewei.hzyjy.xunzhi.ai.api.io.req.AiPropertiesPageReqDTO;
import com.hewei.hzyjy.xunzhi.ai.api.io.req.AiPropertiesUpdateReqDTO;
import com.hewei.hzyjy.xunzhi.ai.api.io.resp.AiModelOptionRespDTO;
import com.hewei.hzyjy.xunzhi.ai.api.io.resp.AiPropertiesRespDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AiPropertiesServiceImpl extends ServiceImpl<AiPropertiesMapper, AiPropertiesDO> implements AiPropertiesService {

    private final ApiKeyCryptoService apiKeyCryptoService;

    // ... (其他方法省略)

    @Override
    public List<AiModelOptionRespDTO> getAvailableAiModels(String username) {
        List<AiPropertiesRespDTO> enabledProperties = getAllEnabledAiProperties(username);
        return enabledProperties.stream()
                .map(prop -> AiModelOptionRespDTO.builder()
                        .id(prop.getId())
                        .aiName(prop.getAiName())
                        .aiType(Integer.valueOf(prop.getAiType()))
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public void createAiProperties(AiPropertiesCreateReqDTO requestParam, String username) {
        if (StrUtil.isBlank(requestParam.getApiKey())) {
            throw new ClientException("API Key不能为空");
        }
        LambdaQueryWrapper<AiPropertiesDO> queryWrapper = Wrappers.lambdaQuery(AiPropertiesDO.class)
                .eq(AiPropertiesDO::getAiName, requestParam.getAiName())
                .eq(AiPropertiesDO::getDelFlag, 0);
        applyVisibleScope(queryWrapper, username);

        if (baseMapper.selectCount(queryWrapper) > 0) {
            throw new ClientException("AI名称已存在");
        }

        AiPropertiesDO aiPropertiesDO = new AiPropertiesDO();
        BeanUtil.copyProperties(requestParam, aiPropertiesDO);
        aiPropertiesDO.setApiKey(apiKeyCryptoService.encrypt(requestParam.getApiKey()));
        aiPropertiesDO.setOwnerUsername(username);
        aiPropertiesDO.setCreateTime(new Date());
        aiPropertiesDO.setUpdateTime(new Date());
        aiPropertiesDO.setDelFlag(0);

        if (aiPropertiesDO.getIsEnabled() == null) {
            aiPropertiesDO.setIsEnabled(1);
        }

        baseMapper.insert(aiPropertiesDO);
    }

    @Override
    public void updateAiProperties(AiPropertiesUpdateReqDTO requestParam, String username) {
        AiPropertiesDO existingRecord = requireOwnedRecord(requestParam.getId(), username);

        if (StrUtil.isNotBlank(requestParam.getAiName()) && !requestParam.getAiName().equals(existingRecord.getAiName())) {
            LambdaQueryWrapper<AiPropertiesDO> queryWrapper = Wrappers.lambdaQuery(AiPropertiesDO.class)
                    .eq(AiPropertiesDO::getAiName, requestParam.getAiName())
                    .eq(AiPropertiesDO::getDelFlag, 0)
                    .ne(AiPropertiesDO::getId, requestParam.getId());
            applyVisibleScope(queryWrapper, username);

            if (baseMapper.selectCount(queryWrapper) > 0) {
                throw new ClientException("AI名称已存在");
            }
        }

        AiPropertiesDO aiPropertiesDO = new AiPropertiesDO();
        BeanUtil.copyProperties(requestParam, aiPropertiesDO);
        // apiKey 为空表示不修改，非空则加密后更新
        if (StrUtil.isBlank(requestParam.getApiKey())) {
            aiPropertiesDO.setApiKey(null);
        } else {
            aiPropertiesDO.setApiKey(apiKeyCryptoService.encrypt(requestParam.getApiKey()));
        }
        aiPropertiesDO.setUpdateTime(new Date());

        baseMapper.updateById(aiPropertiesDO);
    }

    @Override
    public void deleteAiProperties(Long id, String username) {
        requireOwnedRecord(id, username);

        LambdaUpdateWrapper<AiPropertiesDO> updateWrapper = Wrappers.lambdaUpdate(AiPropertiesDO.class)
                .eq(AiPropertiesDO::getId, id)
                .set(AiPropertiesDO::getDelFlag, 1)
                .set(AiPropertiesDO::getUpdateTime, new Date());

        baseMapper.update(null, updateWrapper);
    }

    @Override
    public AiPropertiesRespDTO getAiPropertiesById(Long id, String username) {
        AiPropertiesDO aiPropertiesDO = baseMapper.selectById(id);
        if (aiPropertiesDO == null || aiPropertiesDO.getDelFlag() == 1 || !isVisibleTo(aiPropertiesDO, username)) {
            throw new ClientException("AI配置不存在");
        }

        AiPropertiesRespDTO respDTO = new AiPropertiesRespDTO();
        BeanUtil.copyProperties(aiPropertiesDO, respDTO);

        if (StrUtil.isNotBlank(respDTO.getApiKey())) {
            respDTO.setApiKey(maskApiKey(respDTO.getApiKey()));
        }

        return respDTO;
    }

    @Override
    public IPage<AiPropertiesRespDTO> pageAiProperties(AiPropertiesPageReqDTO requestParam, String username) {
        LambdaQueryWrapper<AiPropertiesDO> queryWrapper = Wrappers.lambdaQuery(AiPropertiesDO.class)
                .eq(AiPropertiesDO::getDelFlag, 0)
                .like(StrUtil.isNotBlank(requestParam.getAiName()), AiPropertiesDO::getAiName, requestParam.getAiName())
                .eq(StrUtil.isNotBlank(requestParam.getAiType()), AiPropertiesDO::getAiType, requestParam.getAiType())
                .eq(requestParam.getIsEnabled() != null, AiPropertiesDO::getIsEnabled, requestParam.getIsEnabled())
                .orderByDesc(AiPropertiesDO::getCreateTime);
        applyVisibleScope(queryWrapper, username);

        IPage<AiPropertiesDO> page = baseMapper.selectPage(requestParam, queryWrapper);

        IPage<AiPropertiesRespDTO> resultPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        List<AiPropertiesRespDTO> records = page.getRecords().stream()
                .map(record -> {
                    AiPropertiesRespDTO respDTO = new AiPropertiesRespDTO();
                    BeanUtil.copyProperties(record, respDTO);
                    if (StrUtil.isNotBlank(respDTO.getApiKey())) {
                        respDTO.setApiKey(maskApiKey(respDTO.getApiKey()));
                    }
                    return respDTO;
                })
                .collect(Collectors.toList());

        resultPage.setRecords(records);
        return resultPage;
    }

    @Override
    public List<AiPropertiesRespDTO> listEnabledAiProperties(String username) {
        LambdaQueryWrapper<AiPropertiesDO> queryWrapper = Wrappers.lambdaQuery(AiPropertiesDO.class)
                .eq(AiPropertiesDO::getDelFlag, 0)
                .eq(AiPropertiesDO::getIsEnabled, 1)
                .orderByDesc(AiPropertiesDO::getCreateTime);
        applyVisibleScope(queryWrapper, username);

        List<AiPropertiesDO> list = baseMapper.selectList(queryWrapper);

        return list.stream()
                .map(record -> {
                    AiPropertiesRespDTO respDTO = new AiPropertiesRespDTO();
                    BeanUtil.copyProperties(record, respDTO);
                    if (StrUtil.isNotBlank(respDTO.getApiKey())) {
                        respDTO.setApiKey(maskApiKey(respDTO.getApiKey()));
                    }
                    return respDTO;
                })
                .collect(Collectors.toList());
    }

    @Override
    public void toggleAiPropertiesStatus(Long id, Integer isEnabled, String username) {
        requireOwnedRecord(id, username);

        LambdaUpdateWrapper<AiPropertiesDO> updateWrapper = Wrappers.lambdaUpdate(AiPropertiesDO.class)
                .eq(AiPropertiesDO::getId, id)
                .set(AiPropertiesDO::getIsEnabled, isEnabled)
                .set(AiPropertiesDO::getUpdateTime, new Date());

        baseMapper.update(null, updateWrapper);
    }

    @Override
    public List<AiPropertiesRespDTO> getAllEnabledAiProperties(String username) {
        return listEnabledAiProperties(username);
    }

    @Override
    public AiPropertiesDO getEnabledByAiType(String aiType) {
        // 默认/平台级配置只从公共行选取，避免命中用户私有 Key
        LambdaQueryWrapper<AiPropertiesDO> queryWrapper = Wrappers.lambdaQuery(AiPropertiesDO.class)
                .eq(AiPropertiesDO::getDelFlag, 0)
                .eq(AiPropertiesDO::getIsEnabled, 1)
                .eq(AiPropertiesDO::getAiType, aiType)
                .isNull(AiPropertiesDO::getOwnerUsername)
                .orderByDesc(AiPropertiesDO::getCreateTime)
                .last("LIMIT 1");

        return baseMapper.selectOne(queryWrapper);
    }

    @Override
    public AiPropertiesDO getDefaultDoubaoConfig() {
        AiPropertiesDO doubaoConfig = getEnabledByAiType("doubao");
        if (doubaoConfig == null) {
            throw new ClientException("豆包AI配置不存在或未启用");
        }
        return doubaoConfig;
    }

    @Override
    public AiPropertiesDO getUsableById(Long aiId, String username) {
        AiPropertiesDO aiProperties = baseMapper.selectById(aiId);
        if (aiProperties == null || aiProperties.getDelFlag() == 1 || aiProperties.getIsEnabled() == 0) {
            throw new ClientException("AI配置不存在或已禁用");
        }
        if (!isVisibleTo(aiProperties, username)) {
            throw new ClientException("无权使用该模型");
        }
        return aiProperties;
    }

    @Override
    public String testConnection(AiPropertiesCreateReqDTO requestParam) {
        if (StrUtil.isBlank(requestParam.getApiKey())) {
            throw new ClientException("API Key不能为空");
        }
        if (StrUtil.isBlank(requestParam.getModelName())) {
            throw new ClientException("模型名称不能为空");
        }
        String baseUrl = requestParam.getApiUrl();
        if (StrUtil.isBlank(baseUrl)) {
            baseUrl = AiPropritiesType.getByType(requestParam.getAiType()).getDefaultBaseUrl();
        }
        if (StrUtil.isBlank(baseUrl)) {
            throw new ClientException("API地址不能为空");
        }

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(10_000);
        requestFactory.setReadTimeout(20_000);
        RestClient restClient = RestClient.builder()
                .baseUrl(baseUrl.replaceAll("/+$", ""))
                .requestFactory(requestFactory)
                .build();

        Map<String, Object> body = Map.of(
                "model", requestParam.getModelName(),
                "messages", List.of(Map.of("role", "user", "content", "ping")),
                "max_tokens", 1,
                "stream", false);

        try {
            restClient.post()
                    .uri("/chat/completions")
                    .header("Authorization", "Bearer " + requestParam.getApiKey())
                    .header("Content-Type", "application/json")
                    .body(body)
                    .retrieve()
                    .body(String.class);
            return "连接成功";
        } catch (RestClientResponseException e) {
            throw new ClientException("连接失败：HTTP " + e.getStatusCode().value() + "，请检查 API Key、地址与模型名称");
        } catch (Exception e) {
            throw new ClientException("连接失败：" + e.getMessage());
        }
    }

    /**
     * 可见范围：公共（owner 为 NULL）+ 当前用户私有
     */
    private void applyVisibleScope(LambdaQueryWrapper<AiPropertiesDO> queryWrapper, String username) {
        if (StrUtil.isBlank(username)) {
            queryWrapper.isNull(AiPropertiesDO::getOwnerUsername);
        } else {
            queryWrapper.and(w -> w.isNull(AiPropertiesDO::getOwnerUsername)
                    .or().eq(AiPropertiesDO::getOwnerUsername, username));
        }
    }

    private boolean isVisibleTo(AiPropertiesDO aiProperties, String username) {
        return aiProperties.getOwnerUsername() == null
                || aiProperties.getOwnerUsername().equals(username);
    }

    /**
     * 仅允许操作本人的配置，公共配置（owner 为 NULL）拒绝
     */
    private AiPropertiesDO requireOwnedRecord(Long id, String username) {
        AiPropertiesDO existingRecord = baseMapper.selectById(id);
        if (existingRecord == null || existingRecord.getDelFlag() == 1) {
            throw new ClientException("AI配置不存在");
        }
        if (existingRecord.getOwnerUsername() == null || !existingRecord.getOwnerUsername().equals(username)) {
            throw new ClientException("无权操作该模型配置");
        }
        return existingRecord;
    }

    private String maskApiKey(String apiKey) {
        if (StrUtil.isBlank(apiKey) || apiKey.length() <= 8) {
            return "****";
        }
        return apiKey.substring(0, 4) + "****" + apiKey.substring(apiKey.length() - 4);
    }
}

