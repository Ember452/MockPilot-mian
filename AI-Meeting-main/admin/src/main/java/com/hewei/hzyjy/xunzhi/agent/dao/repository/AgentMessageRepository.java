package com.hewei.hzyjy.xunzhi.agent.dao.repository;

import com.hewei.hzyjy.xunzhi.agent.dao.entity.AgentMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * AgentMessage MongoDB Repository
 */
@Repository
public interface AgentMessageRepository extends MongoRepository<AgentMessage, String> {
    
    /**
     * 根据会话ID查询消息列表，按消息序号排序
     */
    List<AgentMessage> findBySessionIdAndDelFlagOrderByMessageSeqAsc(String sessionId, Integer delFlag);
    
    /**
     * 分页查询指定会话的消息
     */
    Page<AgentMessage> findBySessionIdAndDelFlagOrderByCreateTimeAsc(String sessionId, Integer delFlag, Pageable pageable);
    
    /**
     * 分页查询所有消息
     */
    Page<AgentMessage> findByDelFlagOrderByCreateTimeDesc(Integer delFlag, Pageable pageable);

    /**
     * 分页查询指定会话集合的消息
     */
    Page<AgentMessage> findBySessionIdInAndDelFlagOrderByCreateTimeDesc(List<String> sessionIds, Integer delFlag, Pageable pageable);
    
    /**
     * 统计会话消息数量
     */
    Integer countBySessionIdAndDelFlag(String sessionId, Integer delFlag);
    
    /**
     * 查询会话中最大的消息序号
     */
    AgentMessage findTopBySessionIdAndDelFlagOrderByMessageSeqDesc(String sessionId, Integer delFlag);
    
    /**
     * 根据会话ID和删除标志查询消息列表
     */
    List<AgentMessage> findBySessionIdAndDelFlag(String sessionId, Integer delFlag);
    
    /**
     * 根据会话ID查询消息列表，按消息序号倒序排序
     */
    List<AgentMessage> findBySessionIdAndDelFlagOrderByMessageSeqDesc(String sessionId, Integer delFlag);
}
