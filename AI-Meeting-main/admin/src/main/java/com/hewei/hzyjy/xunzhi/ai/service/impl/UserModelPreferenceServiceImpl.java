package com.hewei.hzyjy.xunzhi.ai.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hewei.hzyjy.xunzhi.ai.api.io.resp.UserModelPreferenceRespDTO;
import com.hewei.hzyjy.xunzhi.ai.dao.entity.AiPropertiesDO;
import com.hewei.hzyjy.xunzhi.ai.dao.entity.UserModelPreferenceDO;
import com.hewei.hzyjy.xunzhi.ai.dao.mapper.UserModelPreferenceMapper;
import com.hewei.hzyjy.xunzhi.ai.service.AiPropertiesService;
import com.hewei.hzyjy.xunzhi.ai.service.UserModelPreferenceService;
import com.hewei.hzyjy.xunzhi.common.convention.exception.ClientException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserModelPreferenceServiceImpl extends ServiceImpl<UserModelPreferenceMapper, UserModelPreferenceDO>
        implements UserModelPreferenceService {

    private static final Set<String> FEATURE_CODES = Set.of(FEATURE_CHAT, FEATURE_KB_CHAT, FEATURE_REVIEW);

    private final AiPropertiesService aiPropertiesService;

    @Override
    public List<UserModelPreferenceRespDTO> listPreferences(String username) {
        List<UserModelPreferenceDO> records = baseMapper.selectList(
                Wrappers.lambdaQuery(UserModelPreferenceDO.class)
                        .eq(UserModelPreferenceDO::getUsername, username)
                        .eq(UserModelPreferenceDO::getDelFlag, 0));
        return records.stream()
                .map(record -> UserModelPreferenceRespDTO.builder()
                        .featureCode(record.getFeatureCode())
                        .aiId(record.getAiId())
                        .aiName(loadAiNameQuietly(record.getAiId(), username))
                        .build())
                .toList();
    }

    @Override
    public void savePreference(String username, String featureCode, Long aiId) {
        if (StrUtil.isBlank(username)) {
            throw new ClientException("用户未登录");
        }
        if (featureCode == null || !FEATURE_CODES.contains(featureCode)) {
            throw new ClientException("不支持的功能编码");
        }

        UserModelPreferenceDO existing = baseMapper.selectOne(
                Wrappers.lambdaQuery(UserModelPreferenceDO.class)
                        .eq(UserModelPreferenceDO::getUsername, username)
                        .eq(UserModelPreferenceDO::getFeatureCode, featureCode));

        // aiId 为空表示清除绑定（恢复平台默认）
        if (aiId == null) {
            if (existing != null) {
                baseMapper.deleteById(existing.getId());
            }
            return;
        }

        // 归属校验：公共或本人私有的启用配置才可绑定
        aiPropertiesService.getUsableById(aiId, username);

        Date now = new Date();
        if (existing != null) {
            existing.setAiId(aiId);
            existing.setDelFlag(0);
            existing.setUpdateTime(now);
            baseMapper.updateById(existing);
        } else {
            UserModelPreferenceDO record = new UserModelPreferenceDO();
            record.setUsername(username);
            record.setFeatureCode(featureCode);
            record.setAiId(aiId);
            record.setCreateTime(now);
            record.setUpdateTime(now);
            record.setDelFlag(0);
            baseMapper.insert(record);
        }
    }

    @Override
    public AiPropertiesDO resolvePreferred(String username, String featureCode) {
        if (StrUtil.isBlank(username)) {
            return null;
        }
        UserModelPreferenceDO record = baseMapper.selectOne(
                Wrappers.lambdaQuery(UserModelPreferenceDO.class)
                        .eq(UserModelPreferenceDO::getUsername, username)
                        .eq(UserModelPreferenceDO::getFeatureCode, featureCode)
                        .eq(UserModelPreferenceDO::getDelFlag, 0));
        if (record == null || record.getAiId() == null) {
            return null;
        }
        try {
            return aiPropertiesService.getUsableById(record.getAiId(), username);
        } catch (ClientException e) {
            // 绑定的配置已删除/停用/无权使用：静默回退平台默认
            log.warn("User model preference unavailable, username={}, feature={}, aiId={}: {}",
                    username, featureCode, record.getAiId(), e.getMessage());
            return null;
        }
    }

    private String loadAiNameQuietly(Long aiId, String username) {
        try {
            return aiPropertiesService.getUsableById(aiId, username).getAiName();
        } catch (ClientException e) {
            return null;
        }
    }
}
