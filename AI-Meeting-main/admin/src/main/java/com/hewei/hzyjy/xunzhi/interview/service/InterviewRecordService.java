package com.hewei.hzyjy.xunzhi.interview.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.hewei.hzyjy.xunzhi.interview.dao.entity.InterviewRecordDO;
import com.hewei.hzyjy.xunzhi.interview.api.io.req.InterviewRecordPageReqDTO;
import com.hewei.hzyjy.xunzhi.interview.api.io.req.InterviewRecordSaveReqDTO;
import com.hewei.hzyjy.xunzhi.interview.api.io.resp.InterviewRecordRespDTO;

import java.util.Map;

/**
 * 面试记录服务接口
 */
public interface InterviewRecordService extends IService<InterviewRecordDO> {

    /**
     * 保存面试记录
     * @param sessionId 会话ID
     * @param requestParam 保存请求参数
     */
    void saveInterviewRecord(String sessionId, Long userId, InterviewRecordSaveReqDTO requestParam);

    /**
     * 分页查询用户面试记录
     * @param username 用户名
     * @param requestParam 分页查询参数
     * @return 分页结果
     */
    IPage<InterviewRecordRespDTO> pageInterviewRecords(Long userId, InterviewRecordPageReqDTO requestParam);

    /**
     * 根据会话ID获取面试记录
     * @param sessionId 会话ID
     * @param username 当前登录用户名
     * @return 面试记录
     */
    InterviewRecordRespDTO getBySessionId(String sessionId, Long userId);

    /**
     * 从Redis保存面试记录
     * @param sessionId 会话ID
     * @param username 当前登录用户名
     */
    void saveInterviewRecordFromRedis(String sessionId, Long userId);

    /**
     * 解析面试建议字符串为Map格式
     * @param suggestionsString 面试建议字符串（分号分隔）
     * @return 解析后的建议Map，key为编号，value为建议内容
     */
    Map<String, String> parseSuggestionsMap(String suggestionsString);

    /**
     * 删除单条面试记录（软删除）
     */
    void deleteInterviewRecord(String sessionId, Long userId);

    /**
     * 清空用户所有面试记录（软删除）
     */
    void clearAllInterviewRecords(Long userId);
}
