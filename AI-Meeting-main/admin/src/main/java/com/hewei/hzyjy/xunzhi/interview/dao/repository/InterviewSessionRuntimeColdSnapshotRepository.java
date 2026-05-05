package com.hewei.hzyjy.xunzhi.interview.dao.repository;

import com.hewei.hzyjy.xunzhi.interview.dao.entity.InterviewSessionRuntimeColdSnapshot;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 定义面试会话冷快照的持久化访问接口，
 * 用于查询和维护低频材料层的字段级持久化数据。
 *
 * @author 程序员牛肉
 */
@Repository
public interface InterviewSessionRuntimeColdSnapshotRepository extends MongoRepository<InterviewSessionRuntimeColdSnapshot, String>,
        InterviewSessionRuntimeColdSnapshotRepositoryCustom {

    Optional<InterviewSessionRuntimeColdSnapshot> findBySessionId(String sessionId);
}
