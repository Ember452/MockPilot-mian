package com.hewei.hzyjy.xunzhi.ai.service;

import com.hewei.hzyjy.xunzhi.ai.api.io.resp.UserModelPreferenceRespDTO;
import com.hewei.hzyjy.xunzhi.ai.dao.entity.AiPropertiesDO;

import java.util.List;

/**
 * 用户功能级默认模型绑定服务：
 * 用户可为不同功能绑定不同模型，未绑定时由各功能回退平台默认。
 */
public interface UserModelPreferenceService {

    /** AI 对话 */
    String FEATURE_CHAT = "chat";

    /** 知识库对话（RAG） */
    String FEATURE_KB_CHAT = "kb_chat";

    /** 复习弱项抽取 */
    String FEATURE_REVIEW = "review";

    /**
     * 查询当前用户全部功能绑定
     */
    List<UserModelPreferenceRespDTO> listPreferences(String username);

    /**
     * 保存/更新功能绑定；aiId 为空表示清除绑定（恢复平台默认）
     */
    void savePreference(String username, String featureCode, Long aiId);

    /**
     * 解析用户在指定功能绑定的模型配置；
     * 未绑定或绑定的配置已删除/停用/无权使用时返回 null（静默回退，不抛异常）
     */
    AiPropertiesDO resolvePreferred(String username, String featureCode);
}
