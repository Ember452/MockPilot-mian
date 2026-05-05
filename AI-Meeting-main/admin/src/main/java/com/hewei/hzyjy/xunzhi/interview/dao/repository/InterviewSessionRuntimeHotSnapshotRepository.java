package com.hewei.hzyjy.xunzhi.interview.dao.repository;

import com.hewei.hzyjy.xunzhi.interview.dao.entity.InterviewSessionRuntimeHotSnapshot;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 定义面试会话热快照的持久化访问接口，
 * 用于查询和维护高频运行态的字段级持久化数据。
 *
 * @author 程序员牛肉
 */
@Repository
public interface InterviewSessionRuntimeHotSnapshotRepository extends MongoRepository<InterviewSessionRuntimeHotSnapshot, String>,
        InterviewSessionRuntimeHotSnapshotRepositoryCustom {

    Optional<InterviewSessionRuntimeHotSnapshot> findBySessionId(String sessionId);
}
