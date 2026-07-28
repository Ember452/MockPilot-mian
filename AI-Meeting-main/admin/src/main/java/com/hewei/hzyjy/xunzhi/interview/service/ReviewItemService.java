package com.hewei.hzyjy.xunzhi.interview.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.hewei.hzyjy.xunzhi.interview.api.io.req.ReviewItemPageReqDTO;
import com.hewei.hzyjy.xunzhi.interview.api.io.resp.ReviewGenerateRespDTO;
import com.hewei.hzyjy.xunzhi.interview.api.io.resp.ReviewGrowthRespDTO;
import com.hewei.hzyjy.xunzhi.interview.api.io.resp.ReviewItemRespDTO;
import com.hewei.hzyjy.xunzhi.interview.dao.entity.ReviewItemDO;

/**
 * 复习清单服务接口
 */
public interface ReviewItemService extends IService<ReviewItemDO> {

    /**
     * 从面试报告生成复习清单（幂等：已生成直接返回）
     *
     * @param sessionId 面试会话ID
     * @param userId    当前用户ID
     * @param username  当前用户名（用于解析用户绑定的复习模型）
     * @param kbId      可选知识库ID，非空时关联检索参考片段
     */
    ReviewGenerateRespDTO generateFromInterview(String sessionId, Long userId, String username, Long kbId);

    /**
     * 分页查询当前用户复习条目
     */
    IPage<ReviewItemRespDTO> pageReviewItems(Long userId, ReviewItemPageReqDTO requestParam);

    /**
     * 更新条目状态（0=待复习，1=已掌握），校验归属
     */
    void updateStatus(Long userId, Long itemId, Integer status);

    /**
     * 成长曲线：分数时间序 + 弱项知识点聚合
     */
    ReviewGrowthRespDTO growth(Long userId);
}
