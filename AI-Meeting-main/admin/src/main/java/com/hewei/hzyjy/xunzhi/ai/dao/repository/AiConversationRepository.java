package com.hewei.hzyjy.xunzhi.ai.dao.repository;

import com.hewei.hzyjy.xunzhi.ai.dao.entity.AiConversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * AI会话Repository接口
 * @author nageoffer
 */
@Repository
public interface AiConversationRepository extends MongoRepository<AiConversation, String> {
    
    /**
     * 根据会话ID查找会话
     */
    Optional<AiConversation> findBySessionIdAndDelFlag(String sessionId, Integer delFlag);
    
    /**
     * 根据用户名分页查询会话列表
     */
    Page<AiConversation> findByUsernameAndDelFlagOrderByCreateTimeDesc(String username, Integer delFlag, Pageable pageable);
    
    /**
     * 根据用户名和AI配置ID分页查询会话列表
     */
    Page<AiConversation> findByUsernameAndAiIdAndDelFlagOrderByCreateTimeDesc(String username, Long aiId, Integer delFlag, Pageable pageable);
    
    /**
     * 根据用户名查询所有会话
     */
    List<AiConversation> findByUsernameAndDelFlagOrderByCreateTimeDesc(String username, Integer delFlag);
    
    /**
     * 统计用户会话数量
     */
    long countByUsernameAndDelFlag(String username, Integer delFlag);
}