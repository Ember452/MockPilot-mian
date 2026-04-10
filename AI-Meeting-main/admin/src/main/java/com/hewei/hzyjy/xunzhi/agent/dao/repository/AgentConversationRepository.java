package com.hewei.hzyjy.xunzhi.agent.dao.repository;

import com.hewei.hzyjy.xunzhi.agent.dao.entity.AgentConversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

/**
 * AgentConversation MongoDB Repository
 */
@Repository
public interface AgentConversationRepository extends MongoRepository<AgentConversation, String> {
    
    /**
     * 根据会话ID查询会话
     */
    Optional<AgentConversation> findBySessionIdAndDelFlag(String sessionId, Integer delFlag);
    
    /**
     * 分页查询用户会话列表
     */
    Page<AgentConversation> findByUserIdAndDelFlagOrderByUpdateTimeDesc(Long userId, Integer delFlag, Pageable pageable);

    /**
     * 查询用户全部会话
     */
    List<AgentConversation> findByUserIdAndDelFlag(Long userId, Integer delFlag);
    
    /**
     * 分页查询用户指定智能体的会话列表
     */
    Page<AgentConversation> findByUserIdAndAgentIdAndDelFlagOrderByUpdateTimeDesc(Long userId, Long agentId, Integer delFlag, Pageable pageable);
    
    /**
     * 分页查询用户会话列表（支持状态筛选）
     */
    Page<AgentConversation> findByUserIdAndStatusAndDelFlagOrderByUpdateTimeDesc(Long userId, Integer status, Integer delFlag, Pageable pageable);
    
    /**
     * 分页查询用户会话列表（支持智能体和状态筛选）
     */
    Page<AgentConversation> findByUserIdAndAgentIdAndStatusAndDelFlagOrderByUpdateTimeDesc(Long userId, Long agentId, Integer status, Integer delFlag, Pageable pageable);
    
    /**
     * 根据关键词搜索会话标题
     */
    @Query("{'userId': ?0, 'delFlag': ?1, 'conversationTitle': {$regex: ?2, $options: 'i'}}")
    Page<AgentConversation> findByUserIdAndDelFlagAndTitleContaining(Long userId, Integer delFlag, String keyword, Pageable pageable);
    
    /**
     * 根据关键词搜索会话标题（支持智能体筛选）
     */
    @Query("{'userId': ?0, 'agentId': ?1, 'delFlag': ?2, 'conversationTitle': {$regex: ?3, $options: 'i'}}")
    Page<AgentConversation> findByUserIdAndAgentIdAndDelFlagAndTitleContaining(Long userId, Long agentId, Integer delFlag, String keyword, Pageable pageable);
    
    /**
     * 根据关键词搜索会话标题（支持状态筛选）
     */
    @Query("{'userId': ?0, 'status': ?1, 'delFlag': ?2, 'conversationTitle': {$regex: ?3, $options: 'i'}}")
    Page<AgentConversation> findByUserIdAndStatusAndDelFlagAndTitleContaining(Long userId, Integer status, Integer delFlag, String keyword, Pageable pageable);
    
    /**
     * 根据关键词搜索会话标题（支持智能体和状态筛选）
     */
    @Query("{'userId': ?0, 'agentId': ?1, 'status': ?2, 'delFlag': ?3, 'conversationTitle': {$regex: ?4, $options: 'i'}}")
    Page<AgentConversation> findByUserIdAndAgentIdAndStatusAndDelFlagAndTitleContaining(Long userId, Long agentId, Integer status, Integer delFlag, String keyword, Pageable pageable);
}
