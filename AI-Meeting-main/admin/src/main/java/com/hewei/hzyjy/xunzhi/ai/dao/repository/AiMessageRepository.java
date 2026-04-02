package com.hewei.hzyjy.xunzhi.ai.dao.repository;

import com.hewei.hzyjy.xunzhi.ai.dao.entity.AiMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * AI消息Repository接口
 * @author nageoffer
 */
@Repository
public interface AiMessageRepository extends MongoRepository<AiMessage, String> {
    
    /**
     * 根据会话ID查询消息列表，按序号升序排列
     */
    List<AiMessage> findBySessionIdAndDelFlagOrderByMessageSeqAsc(String sessionId, Integer delFlag);
    
    /**
     * 根据会话ID分页查询消息列表
     */
    Page<AiMessage> findBySessionIdAndDelFlagOrderByCreateTimeAsc(String sessionId, Integer delFlag, Pageable pageable);
    
    /**
     * 分页查询所有消息
     */
    Page<AiMessage> findByDelFlagOrderByCreateTimeDesc(Integer delFlag, Pageable pageable);

    Page<AiMessage> findBySessionIdInAndDelFlagOrderByCreateTimeDesc(List<String> sessionIds, Integer delFlag, Pageable pageable);
    
    /**
     * 统计会话消息数量
     */
    long countBySessionIdAndDelFlag(String sessionId, Integer delFlag);
    
    /**
     * 获取会话中最大的消息序号
     */
    AiMessage findTopBySessionIdAndDelFlagOrderByMessageSeqDesc(String sessionId, Integer delFlag);
    
    /**
     * 删除会话的所有消息（软删除）
     */
    void deleteBySessionId(String sessionId);
}
