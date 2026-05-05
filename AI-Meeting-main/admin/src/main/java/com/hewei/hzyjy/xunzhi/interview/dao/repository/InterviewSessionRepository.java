package com.hewei.hzyjy.xunzhi.interview.dao.repository;

import com.hewei.hzyjy.xunzhi.interview.dao.entity.InterviewSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface InterviewSessionRepository extends MongoRepository<InterviewSession, String> {

    Optional<InterviewSession> findBySessionIdAndDelFlag(String sessionId, Integer delFlag);

    List<InterviewSession> findByUserIdAndStatusInAndDelFlagOrderByUpdateTimeDesc(
            Long userId,
            Collection<String> statusList,
            Integer delFlag
    );

    Page<InterviewSession> findByUserIdAndDelFlagOrderByUpdateTimeDesc(Long userId, Integer delFlag, Pageable pageable);

    Page<InterviewSession> findByUserIdAndStatusAndDelFlagOrderByUpdateTimeDesc(
            Long userId,
            String status,
            Integer delFlag,
            Pageable pageable
    );

    @Query("{'userId': ?0, 'delFlag': ?1, 'conversationTitle': {$regex: ?2, $options: 'i'}}")
    Page<InterviewSession> findByUserIdAndDelFlagAndTitleContaining(
            Long userId,
            Integer delFlag,
            String keyword,
            Pageable pageable
    );

    @Query("{'userId': ?0, 'status': ?1, 'delFlag': ?2, 'conversationTitle': {$regex: ?3, $options: 'i'}}")
    Page<InterviewSession> findByUserIdAndStatusAndDelFlagAndTitleContaining(
            Long userId,
            String status,
            Integer delFlag,
            String keyword,
            Pageable pageable
    );
}
