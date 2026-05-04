package com.hewei.hzyjy.xunzhi.interview.dao.repository;

import com.hewei.hzyjy.xunzhi.interview.dao.entity.InterviewQuestion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * InterviewQuestion MongoDB Repository
 */
@Repository
public interface InterviewQuestionRepository extends MongoRepository<InterviewQuestion, String> {
    
    /**
     * 根据会话ID查询面试题
     */
    Optional<InterviewQuestion> findBySessionIdAndDelFlag(String sessionId, Integer delFlag);
    
    /**
     * 根据用户名查询面试题列表
     */
    List<InterviewQuestion> findByUserNameAndDelFlagOrderByCreateTimeDesc(String userName, Integer delFlag);
    
    /**
     * 分页查询指定用户的面试题
     */
    Page<InterviewQuestion> findByUserNameAndDelFlagOrderByCreateTimeDesc(String userName, Integer delFlag, Pageable pageable);
    
    /**
     * 分页查询所有面试题
     */
    Page<InterviewQuestion> findByDelFlagOrderByCreateTimeDesc(Integer delFlag, Pageable pageable);
    
    /**
     * 根据面试类型查询面试题列表
     */
    List<InterviewQuestion> findByInterviewTypeAndDelFlagOrderByCreateTimeDesc(String interviewType, Integer delFlag);
    
    /**
     * 根据智能体ID查询面试题列表
     */
    List<InterviewQuestion> findByAgentIdAndDelFlagOrderByCreateTimeDesc(Long agentId, Integer delFlag);
    
    /**
     * 统计用户的面试题数量
     */
    Integer countByUserNameAndDelFlag(String userName, Integer delFlag);
    
    /**
     * 统计指定面试类型的数量
     */
    Integer countByInterviewTypeAndDelFlag(String interviewType, Integer delFlag);
}