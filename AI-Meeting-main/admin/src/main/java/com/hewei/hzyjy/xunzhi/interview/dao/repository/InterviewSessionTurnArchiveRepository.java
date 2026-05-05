package com.hewei.hzyjy.xunzhi.interview.dao.repository;

import com.hewei.hzyjy.xunzhi.interview.dao.entity.InterviewSessionTurnArchive;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 定义面试会话轮次归档的持久化访问接口，
 * 用于按会话和请求维度查询轮次归档并支撑回放与恢复场景。
 *
 * @author 程序员牛肉
 */
@Repository
public interface InterviewSessionTurnArchiveRepository extends MongoRepository<InterviewSessionTurnArchive, String> {

    List<InterviewSessionTurnArchive> findBySessionIdOrderBySeqAsc(String sessionId);

    Optional<InterviewSessionTurnArchive> findFirstBySessionIdOrderBySeqDesc(String sessionId);

    Optional<InterviewSessionTurnArchive> findBySessionIdAndRequestId(String sessionId, String requestId);
}
